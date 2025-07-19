package com.focusfloat.app.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

enum class AppRowState {
    Normal,
    Paused,
    Protected,
    Hidden,
}

enum class BannerKind {
    Setup,
    Info,
    Failed,
}

@Composable
fun FocusScreen(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .background(FocusTheme.colors.bg),
    ) {
        content()
    }
}

@Composable
fun AppTextRow(
    name: String,
    modifier: Modifier = Modifier,
    state: AppRowState = AppRowState.Normal,
    meta: String? = null,
    favorite: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val colors = FocusTheme.colors
    val height = if (favorite) FocusTheme.spacing.favRowH else FocusTheme.spacing.rowH
    val textColor = when (state) {
        AppRowState.Normal -> colors.text
        AppRowState.Paused,
        AppRowState.Protected -> colors.text2
        AppRowState.Hidden -> colors.text3
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .run {
                if (onClick != null && enabled) {
                    clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    this
                }
            }
            .padding(horizontal = FocusTheme.spacing.screenPad),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = textColor,
                style = FocusTheme.type.appRow,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!meta.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = meta,
                    color = when (state) {
                        AppRowState.Hidden -> colors.text3
                        else -> colors.text2
                    },
                    style = FocusTheme.type.caption,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (state == AppRowState.Paused) {
            StatusDot(kind = DotKind.Hollow)
        }
    }
}

enum class DotKind {
    Ready,
    Hollow,
    Failed,
}

@Composable
fun StatusDot(kind: DotKind, modifier: Modifier = Modifier) {
    val colors = FocusTheme.colors
    val color = when (kind) {
        DotKind.Ready -> colors.text
        DotKind.Hollow -> colors.dot
        DotKind.Failed -> colors.err
    }
    Canvas(modifier = modifier.size(8.dp)) {
        when (kind) {
            DotKind.Hollow -> drawCircle(
                color = color,
                radius = size.minDimension / 2f - 1.dp.toPx(),
                style = Stroke(width = 1.25.dp.toPx()),
            )
            DotKind.Ready,
            DotKind.Failed -> drawCircle(color = color)
        }
    }
}

@Composable
fun ActionLine(
    text: String,
    modifier: Modifier = Modifier,
    sub: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val colors = FocusTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(width = 0.5.dp, color = colors.line, shape = RoundedCornerShape(0.dp))
            .run {
                if (onClick != null && enabled) clickable(onClick = onClick) else this
            }
            .padding(horizontal = FocusTheme.spacing.screenPad, vertical = 18.dp),
    ) {
        Text(
            text = text,
            color = if (enabled) colors.text else colors.text3,
            style = FocusTheme.type.button,
        )
        if (!sub.isNullOrBlank()) {
            Spacer(Modifier.height(5.dp))
            Text(text = sub, color = colors.text2, style = FocusTheme.type.caption)
        }
    }
}

@Composable
fun StatusBanner(
    title: String,
    modifier: Modifier = Modifier,
    sub: String? = null,
    subMaxLines: Int = Int.MAX_VALUE,
    action: String? = null,
    kind: BannerKind = BannerKind.Setup,
    onAction: (() -> Unit)? = null,
) {
    val colors = FocusTheme.colors
    val failed = kind == BannerKind.Failed
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = FocusTheme.spacing.screenPad)
            .background(
                color = if (failed) colors.errDim else colors.surface,
                shape = RoundedCornerShape(FocusTheme.spacing.radiusBanner),
            )
            .border(
                width = 0.5.dp,
                color = if (failed) Color.Transparent else colors.line,
                shape = RoundedCornerShape(FocusTheme.spacing.radiusBanner),
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        StatusDot(
            kind = if (failed) DotKind.Failed else DotKind.Hollow,
            modifier = Modifier.padding(top = 6.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (failed) colors.err else colors.text,
                style = FocusTheme.type.body,
            )
            if (!sub.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = sub,
                    color = colors.text2,
                    style = FocusTheme.type.caption,
                    maxLines = subMaxLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!action.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = action,
                    color = if (failed) colors.err else colors.text,
                    style = FocusTheme.type.button,
                    modifier = if (onAction != null) Modifier.clickable(onClick = onAction) else Modifier,
                )
            }
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = FocusTheme.colors.text2,
        style = FocusTheme.type.section,
        modifier = modifier.padding(horizontal = FocusTheme.spacing.screenPad),
    )
}

@Composable
fun TopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = FocusTheme.spacing.screenPad),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Text(
                text = "<",
                color = FocusTheme.colors.text,
                style = FocusTheme.type.appRow,
                modifier = Modifier
                    .size(48.dp)
                    .clickable(onClick = onBack)
                    .padding(top = 4.dp),
            )
        }
        Text(
            text = title,
            color = FocusTheme.colors.text,
            style = FocusTheme.type.sheetTitle,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) trailing()
    }
}

@Composable
fun SettingsRow(
    label: String,
    modifier: Modifier = Modifier,
    sub: String? = null,
    value: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(FocusTheme.spacing.rowH)
            .run { if (onClick != null) clickable(onClick = onClick) else this }
            .padding(horizontal = FocusTheme.spacing.screenPad),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = FocusTheme.colors.text,
                style = FocusTheme.type.body,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!sub.isNullOrBlank()) {
                Text(
                    text = sub,
                    color = FocusTheme.colors.text2,
                    style = FocusTheme.type.caption,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (!value.isNullOrBlank()) {
            Text(
                text = value,
                color = FocusTheme.colors.text2,
                style = FocusTheme.type.caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun BatteryArc(
    pct: Float,
    modifier: Modifier = Modifier,
) {
    val text = FocusTheme.colors.text
    val text2 = FocusTheme.colors.text2
    Canvas(modifier = modifier.size(34.dp)) {
        val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
        drawArc(
            color = Color.White.copy(alpha = 0.12f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = stroke,
        )
        drawArc(
            color = text2,
            startAngle = -90f,
            sweepAngle = 360f * pct.coerceIn(0f, 1f),
            useCenter = false,
            style = stroke,
        )
        drawCircle(
            color = text,
            radius = 1.5.dp.toPx(),
            center = Offset(size.width / 2f, size.height / 2f),
        )
    }
}
