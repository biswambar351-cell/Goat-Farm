package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.FarmProfile
import com.example.util.FarmLogoImageHelper

data class LogoPaletteOption(
    val index: Int,
    val label: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val contentColor: Color
)

data class LogoEmblemOption(
    val id: String,
    val label: String,
    val icon: ImageVector
)

data class LogoShapeOption(
    val id: String,
    val label: String
)

object FarmLogoDefaults {
    val paletteOptions = listOf(
        LogoPaletteOption(
            index = 0,
            label = "Forest",
            primaryColor = Color(0xFF1E3A2F),
            secondaryColor = Color(0xFF2C5343),
            contentColor = Color(0xFFD4E8DC)
        ),
        LogoPaletteOption(
            index = 1,
            label = "Harvest",
            primaryColor = Color(0xFF9C6413),
            secondaryColor = Color(0xFFB87D28),
            contentColor = Color(0xFFFFF8E1)
        ),
        LogoPaletteOption(
            index = 2,
            label = "Meadow",
            primaryColor = Color(0xFF1B5E20),
            secondaryColor = Color(0xFF388E3C),
            contentColor = Color(0xFFE8F5E9)
        ),
        LogoPaletteOption(
            index = 3,
            label = "Barn",
            primaryColor = Color(0xFF9A2B12),
            secondaryColor = Color(0xFFC64726),
            contentColor = Color(0xFFFFEDE8)
        ),
        LogoPaletteOption(
            index = 4,
            label = "Cobalt",
            primaryColor = Color(0xFF1E3A8A),
            secondaryColor = Color(0xFF3B5BDB),
            contentColor = Color(0xFFE8EEFF)
        ),
        LogoPaletteOption(
            index = 5,
            label = "Slate",
            primaryColor = Color(0xFF37474F),
            secondaryColor = Color(0xFF546E7A),
            contentColor = Color(0xFFECEFF1)
        )
    )

    val emblemOptions = listOf(
        LogoEmblemOption("goat_pets", "Herd", Icons.Default.Pets),
        LogoEmblemOption("pasture_eco", "Pasture", Icons.Default.Eco),
        LogoEmblemOption("farm_home", "Barn", Icons.Default.Home),
        LogoEmblemOption("agriculture", "Farm", Icons.Default.Agriculture),
        LogoEmblemOption("shield", "Crest", Icons.Default.Shield),
        LogoEmblemOption("award", "Ribbon", Icons.Default.WorkspacePremium)
    )

    val shapeOptions = listOf(
        LogoShapeOption("circle", "Circle"),
        LogoShapeOption("rounded", "Rounded"),
        LogoShapeOption("badge", "Leaf Crest")
    )

    fun paletteAt(index: Int): LogoPaletteOption =
        paletteOptions.getOrElse(index) { paletteOptions.first() }

    fun emblemFor(id: String): LogoEmblemOption =
        emblemOptions.firstOrNull { it.id == id } ?: emblemOptions.first()

    fun shapeFor(id: String, size: Dp): Shape = when (id) {
        "rounded" -> RoundedCornerShape((size.value * 0.26f).dp)
        "badge" -> RoundedCornerShape(
            topStart = (size.value * 0.42f).dp,
            topEnd = (size.value * 0.14f).dp,
            bottomStart = (size.value * 0.14f).dp,
            bottomEnd = (size.value * 0.42f).dp
        )
        else -> CircleShape
    }
}

/**
 * Renders the farmer's customized logo (default launcher artwork, vector emblem + monogram,
 * or user-picked gallery photo) with an optional edit badge and accessible touch target.
 */
@Composable
fun FarmLogoView(
    profile: FarmProfile,
    size: Dp = 48.dp,
    showEditBadge: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val shape = remember(profile.logoShape, size) {
        FarmLogoDefaults.shapeFor(profile.logoShape, size)
    }
    val customBitmap = remember(profile.logoType, profile.logoImageBase64) {
        if (profile.logoType == "photo" && profile.logoImageBase64.isNotBlank()) {
            FarmLogoImageHelper.decodeBase64ToImageBitmap(profile.logoImageBase64)
        } else {
            null
        }
    }
    val palette = remember(profile.logoColorIndex) {
        FarmLogoDefaults.paletteAt(profile.logoColorIndex)
    }
    val emblem = remember(profile.logoEmblemId) {
        FarmLogoDefaults.emblemFor(profile.logoEmblemId)
    }
    val cdText = stringResource(R.string.cd_farm_logo)

    val clickableModifier = if (onClick != null) {
        Modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier.then(clickableModifier),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(shape)
                .border(
                    width = if (size >= 48.dp) 2.dp else 1.5.dp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.28f),
                    shape = shape
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                customBitmap != null -> {
                    Image(
                        bitmap = customBitmap,
                        contentDescription = cdText,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("farm_logo_photo_image")
                    )
                }

                profile.logoType == "emblem" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(palette.primaryColor, palette.secondaryColor)
                                )
                            )
                            .testTag("farm_logo_emblem_container"),
                        contentAlignment = Alignment.Center
                    ) {
                        val initials = profile.logoInitials.trim().uppercase().take(4)
                        if (initials.isNotEmpty()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (size >= 44.dp) {
                                    Icon(
                                        imageVector = emblem.icon,
                                        contentDescription = cdText,
                                        tint = palette.contentColor.copy(alpha = 0.85f),
                                        modifier = Modifier.size((size.value * 0.28f).dp)
                                    )
                                }
                                Text(
                                    text = initials,
                                    color = palette.contentColor,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = when {
                                        initials.length >= 3 -> (size.value * 0.27f).sp
                                        else -> (size.value * 0.34f).sp
                                    },
                                    maxLines = 1
                                )
                            }
                        } else {
                            Icon(
                                imageVector = emblem.icon,
                                contentDescription = cdText,
                                tint = palette.contentColor,
                                modifier = Modifier.size((size.value * 0.56f).dp)
                            )
                        }
                    }
                }

                else -> {
                    Image(
                        painter = painterResource(id = R.drawable.ic_goat_launcher_1794144293500),
                        contentDescription = cdText,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("farm_logo_default_image")
                    )
                }
            }
        }

        if (showEditBadge) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(22.dp)
                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

/**
 * Interactive Logo Studio dialog allowing farmers to design a custom vector + monogram emblem,
 * pick a photo from device gallery via zero-permission Android Photo Picker, or reset to default.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditLogoDialog(
    currentProfile: FarmProfile,
    onDismiss: () -> Unit,
    onSave: (FarmProfile) -> Unit
) {
    val context = LocalContext.current

    var logoType by remember(currentProfile) {
        mutableStateOf(
            if (currentProfile.logoType in listOf("default", "emblem", "photo")) currentProfile.logoType
            else "default"
        )
    }
    var emblemId by remember(currentProfile) {
        mutableStateOf(currentProfile.logoEmblemId.ifBlank { "goat_pets" })
    }
    var colorIndex by remember(currentProfile) {
        mutableIntStateOf(currentProfile.logoColorIndex.coerceIn(0, FarmLogoDefaults.paletteOptions.lastIndex))
    }
    var initials by remember(currentProfile) {
        mutableStateOf(currentProfile.logoInitials)
    }
    var shapeId by remember(currentProfile) {
        mutableStateOf(currentProfile.logoShape.ifBlank { "circle" })
    }
    var imageBase64 by remember(currentProfile) {
        mutableStateOf(currentProfile.logoImageBase64)
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val imageErrorText = stringResource(R.string.error_logo_image_load)

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val encoded = FarmLogoImageHelper.loadScaledLogoBase64FromUri(context, uri)
            if (encoded != null) {
                imageBase64 = encoded
                logoType = "photo"
                errorMessage = null
            } else {
                errorMessage = imageErrorText
            }
        }
    }

    val previewProfile = remember(
        currentProfile,
        logoType,
        emblemId,
        colorIndex,
        initials,
        shapeId,
        imageBase64
    ) {
        currentProfile.copy(
            logoType = logoType,
            logoEmblemId = emblemId,
            logoColorIndex = colorIndex,
            logoInitials = initials,
            logoShape = shapeId,
            logoImageBase64 = imageBase64
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .widthIn(max = 520.dp)
                .testTag("edit_logo_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Text(
                    text = stringResource(R.string.title_edit_logo),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.subtitle_edit_logo),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Live Preview Header Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        FarmLogoView(
                            profile = previewProfile,
                            size = 76.dp,
                            showEditBadge = false,
                            modifier = Modifier.testTag("edit_logo_preview")
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentProfile.farmName.ifBlank { "Khulana Goat Farm" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = when (logoType) {
                                    "photo" -> "Custom Gallery Photo"
                                    "emblem" -> "Custom Emblem • ${FarmLogoDefaults.emblemFor(emblemId).label}"
                                    else -> "Default App Artwork"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable customization controls
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Quick Action Row: Custom Emblem / Pick Photo / Reset Default
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                logoType = "emblem"
                                errorMessage = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("logo_mode_emblem_button"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Emblem", maxLines = 1)
                        }

                        FilledTonalButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("logo_pick_photo_button"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.action_upload_photo), maxLines = 1)
                        }

                        TextButton(
                            onClick = {
                                logoType = "default"
                                shapeId = "circle"
                                imageBase64 = ""
                                initials = ""
                                errorMessage = null
                            },
                            modifier = Modifier.testTag("logo_reset_default_button"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset", maxLines = 1)
                        }
                    }

                    // Shape selector (applies to all logo modes!)
                    Text(
                        text = "Logo Shape",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FarmLogoDefaults.shapeOptions.forEach { option ->
                            FilterChip(
                                selected = shapeId == option.id,
                                onClick = { shapeId = option.id },
                                label = { Text(option.label) },
                                modifier = Modifier.testTag("logo_shape_${option.id}")
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Emblem Icon Picker
                    Text(
                        text = "Emblem Icon",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FarmLogoDefaults.emblemOptions.forEach { option ->
                            val selected = logoType == "emblem" && emblemId == option.id
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    logoType = "emblem"
                                    emblemId = option.id
                                    errorMessage = null
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = option.icon,
                                        contentDescription = option.label,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                label = { Text(option.label) },
                                modifier = Modifier.testTag("logo_emblem_${option.id}")
                            )
                        }
                    }

                    // Brand Color Swatches
                    Text(
                        text = "Badge Color",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FarmLogoDefaults.paletteOptions.forEach { option ->
                            val isSelected = logoType == "emblem" && colorIndex == option.index
                            Surface(
                                onClick = {
                                    logoType = "emblem"
                                    colorIndex = option.index
                                    errorMessage = null
                                },
                                shape = CircleShape,
                                color = option.primaryColor,
                                border = if (isSelected) {
                                    BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
                                } else {
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("logo_color_${option.index}")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "${option.label} selected",
                                            tint = option.contentColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Optional Monogram Initials
                    OutlinedTextField(
                        value = initials,
                        onValueChange = { raw ->
                            val filtered = raw.filter { it.isLetterOrDigit() }.uppercase().take(4)
                            initials = filtered
                            if (filtered.isNotEmpty() && logoType == "default") {
                                logoType = "emblem"
                            }
                        },
                        label = { Text("Monogram Initials (Optional, max 4)") },
                        placeholder = { Text("e.g., KGF") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("logo_initials_input")
                    )
                }

                // Error banner above footer actions
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.testTag("edit_logo_error_banner")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("edit_logo_cancel_button")
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(previewProfile) },
                        modifier = Modifier.testTag("edit_logo_save_button")
                    ) {
                        Text(stringResource(R.string.action_save_logo))
                    }
                }
            }
        }
    }
}
