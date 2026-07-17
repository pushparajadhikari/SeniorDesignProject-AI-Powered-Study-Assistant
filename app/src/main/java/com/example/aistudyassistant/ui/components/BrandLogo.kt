package com.example.aistudyassistant.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.aistudyassistant.R

// ── StudyAI brand marks ───────────────────────────────────────────────────────
//
// Both logo assets carry white *inside* the artwork — the wordmark's letter counters
// and the "AI" badge text are white, and the mark is anti-aliased against white — so
// neither can be keyed out or drawn straight onto the brand gradient. Every helper
// below keeps the artwork on a white surface. Use BrandLogoBadge / BrandBanner over
// the gradient; BrandLogoMark only on already-white surfaces such as a TopAppBar.

/** Logo mark on its own. Only use on a white surface. */
@Composable
fun BrandLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp
) {
    Image(
        painter            = painterResource(R.drawable.logo_mark),
        contentDescription = null,   // decorative — the screen title carries the name
        modifier           = modifier.size(size)
    )
}

/** Logo mark on a white circle, so it stays legible over the brand gradient. */
@Composable
fun BrandLogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    Box(
        modifier         = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        BrandLogoMark(size = size * 0.6f)
    }
}

/** Full "STUDY AI" banner on a white plate, for the gradient welcome/login headers. */
@Composable
fun BrandBanner(
    modifier: Modifier = Modifier,
    width: Dp = 220.dp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        // Height follows the artwork's aspect ratio from the fixed width.
        Image(
            painter            = painterResource(R.drawable.logo_banner),
            contentDescription = "StudyAI — Smart Notes. Smarter You.",
            modifier           = Modifier.width(width)
        )
    }
}
