package ca.michaelmaguire.dialett.ui

import androidx.compose.runtime.Composable

/** The system's back gesture or button (Android); nothing on platforms without one. */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
