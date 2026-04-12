package com.gymbro.core.ui.theme

import androidx.compose.ui.graphics.Color

// Base Backgrounds
val Background = Color(0xFF0A0A0A)
val SurfacePrimary = Color(0xFF141414)
val Surface = Color(0xFF1C1C1E)
val SurfaceVariant = Color(0xFF2C2C2E)

// Gradient Accent Colors
val AccentGreenStart = Color(0xFF00FF87)
val AccentGreenEnd = Color(0xFF00D9B5)
val AccentCyanStart = Color(0xFF00D4FF)
val AccentCyanEnd = Color(0xFF0091FF)
val AccentAmberStart = Color(0xFFFFB800)
val AccentAmberEnd = Color(0xFFFF8A00)
val AccentRed = Color(0xFFFF3B30)

// Glassmorphic Overlays — 25% opacity meets WCAG AA 4.5:1 contrast on dark backgrounds
val GlassOverlay = Color(0x40FFFFFF)
val GlassBorder = Color(0x33FFFFFF)

// Backwards compatibility aliases
val AccentGreen = AccentGreenStart
val AccentCyan = AccentCyanStart
val AccentAmber = AccentAmberStart

// Text Colors
val OnBackground = Color(0xFFE0E0E0)
val OnSurface = Color(0xFFFFFFFF)
val OnSurfaceVariant = Color(0xFF9E9E9E)

// Error Colors
val Error = AccentRed
val OnError = Color(0xFF000000)

// Light Theme Colors
val BackgroundLight = Color(0xFFF5F5F5)
val SurfacePrimaryLight = Color(0xFFFFFFFF)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFF0F0F0)
val OnBackgroundLight = Color(0xFF1C1C1E)
val OnSurfaceLight = Color(0xFF000000)
val OnSurfaceVariantLight = Color(0xFF616161)
