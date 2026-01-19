# -*- coding: utf-8 -*-

from odoo.http import request, Response
from datetime import datetime
import json
import logging
import time

_logger = logging.getLogger(__name__)

# Rate limiting storage (in-memory, resets on restart)
_rate_limit_store = {}
RATE_LIMIT_REQUESTS = 100  # requests per window
RATE_LIMIT_WINDOW = 60  # seconds


def api_authenticate():
    """
    Authenticate API request using Authorization header.
    Supports both "Bearer <key>" and raw key formats.
    Returns True if authenticated, False otherwise.
    """
    api_key = request.httprequest.headers.get('Authorization')
    if not api_key:
        _logger.warning("Failed API authentication! No 'Authorization' header or empty!")
        return False

    # Support both "Bearer <key>" and raw key formats
    api_key = api_key.replace('Bearer ', '').strip()

    user_id = request.env['res.users.apikeys']._check_credentials(scope='rpc', key=api_key)

    if user_id:
        request.env.user = request.env['res.users'].browse(user_id)
        request.env.uid = user_id
        return True
    else:
        _logger.warning("Failed API authentication! Invalid API key")
        return False


def get_pagination_params():
    """
    Extract pagination parameters from query string.
    Returns (limit, offset) tuple.

    Query params:
    - limit: max records to return (default: 100, max: 1000)
    - offset: number of records to skip (default: 0)
    """
    try:
        limit = int(request.httprequest.args.get('limit', 100))
        limit = min(max(limit, 1), 1000)  # Clamp between 1 and 1000
    except (ValueError, TypeError):
        limit = 100

    try:
        offset = int(request.httprequest.args.get('offset', 0))
        offset = max(offset, 0)  # Ensure non-negative
    except (ValueError, TypeError):
        offset = 0

    return limit, offset


def json_response(data, status=200):
    """Create a JSON response with proper content type."""
    return Response(
        json.dumps(data, default=str),
        status=status,
        content_type='application/json'
    )


def json_error(message, status=400, details=None):
    """Create a JSON error response."""
    error_data = {"error": message}
    if details:
        error_data["details"] = details
    return Response(
        json.dumps(error_data),
        status=status,
        content_type='application/json'
    )


def paginated_response(data, total, limit, offset):
    """Create a paginated JSON response with metadata."""
    return json_response({
        "total": total,
        "limit": limit,
        "offset": offset,
        "count": len(data),
        "data": data
    })


def validate_required_fields(data, required_fields):
    """
    Validate that all required fields are present in data.
    Returns (is_valid, error_response) tuple.
    """
    missing = []
    for field in required_fields:
        if field not in data or data[field] is None:
            missing.append(field)

    if missing:
        return False, json_error(
            "Missing required fields",
            status=400,
            details={"missing_fields": missing}
        )
    return True, None


def validate_foreign_key(model, record_id, field_name):
    """
    Validate that a foreign key reference exists.
    Returns (is_valid, error_response) tuple.
    """
    if not record_id:
        return True, None  # Allow None/False values

    record = request.env[model].sudo().browse(record_id).exists()
    if not record:
        return False, json_error(
            f"Invalid {field_name}",
            status=400,
            details={field_name: f"Record with id {record_id} does not exist in {model}"}
        )
    return True, None


def check_rate_limit():
    """
    Check if the current request exceeds rate limits.
    Returns (is_allowed, error_response) tuple.
    """
    # Use API key or IP as identifier
    api_key = request.httprequest.headers.get('Authorization', '')
    client_id = api_key if api_key else request.httprequest.remote_addr

    current_time = time.time()
    window_start = current_time - RATE_LIMIT_WINDOW

    # Clean old entries and get current count
    if client_id in _rate_limit_store:
        _rate_limit_store[client_id] = [
            t for t in _rate_limit_store[client_id] if t > window_start
        ]
    else:
        _rate_limit_store[client_id] = []

    request_count = len(_rate_limit_store[client_id])

    if request_count >= RATE_LIMIT_REQUESTS:
        return False, json_error(
            "Rate limit exceeded",
            status=429,
            details={
                "limit": RATE_LIMIT_REQUESTS,
                "window_seconds": RATE_LIMIT_WINDOW,
                "retry_after": int(RATE_LIMIT_WINDOW - (current_time - _rate_limit_store[client_id][0]))
            }
        )

    _rate_limit_store[client_id].append(current_time)
    return True, None


def parse_datetime(date_str, format='%Y-%m-%dT%H:%M:%S'):
    """
    Parse a datetime string in ISO 8601 format.
    Returns (datetime_obj, error_response) tuple.
    """
    if not date_str:
        return None, None

    # Support multiple formats for flexibility
    formats = [
        '%Y-%m-%dT%H:%M:%S',  # ISO 8601
        '%Y-%m-%dT%H:%M:%SZ',  # ISO 8601 with Z
        '%Y-%m-%d %H:%M:%S',  # Legacy format
        '%Y-%m-%d',  # Date only
    ]

    for fmt in formats:
        try:
            return datetime.strptime(date_str, fmt), None
        except ValueError:
            continue

    return None, json_error(
        "Invalid datetime format",
        status=400,
        details={"value": date_str, "expected_format": "ISO 8601 (YYYY-MM-DDTHH:MM:SS)"}
    )


def format_datetime(dt):
    """Format a datetime object to ISO 8601 string."""
    if not dt:
        return None
    if isinstance(dt, str):
        return dt
    return dt.strftime('%Y-%m-%dT%H:%M:%S')


def format_date(d):
    """Format a date object to ISO 8601 string."""
    if not d:
        return None
    if isinstance(d, str):
        return d
    return d.strftime('%Y-%m-%d')


def get_filter_params(allowed_filters):
    """
    Extract filter parameters from query string.
    Returns a dict of filter field -> value.

    allowed_filters: dict of {param_name: (odoo_field, type)}
    type can be: 'int', 'str', 'date', 'datetime', 'bool'
    """
    filters = {}
    args = request.httprequest.args

    for param, (field, field_type) in allowed_filters.items():
        value = args.get(param)
        if value is None:
            continue

        try:
            if field_type == 'int':
                filters[field] = int(value)
            elif field_type == 'str':
                filters[field] = value
            elif field_type == 'date':
                dt, _ = parse_datetime(value)
                if dt:
                    filters[field] = dt.date()
            elif field_type == 'datetime':
                dt, _ = parse_datetime(value)
                if dt:
                    filters[field] = dt
            elif field_type == 'bool':
                filters[field] = value.lower() in ('true', '1', 'yes')
        except (ValueError, TypeError):
            pass  # Skip invalid filter values

    return filters


def build_domain(base_domain, filters):
    """
    Build an Odoo search domain from base domain and filters.
    """
    domain = list(base_domain)
    for field, value in filters.items():
        domain.append((field, '=', value))
    return domain
