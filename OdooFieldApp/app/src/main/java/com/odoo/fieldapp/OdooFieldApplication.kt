package com.odoo.fieldapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class with Hilt support
 * 
 * @HiltAndroidApp triggers Hilt's code generation and is required at the application level
 */
@HiltAndroidApp
class OdooFieldApplication : Application()
