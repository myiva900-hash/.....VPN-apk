package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.AppScreen
import com.example.ui.theme.VpnRedPrimary
import com.example.ui.theme.VpnWhatsAppGreen

@Composable
fun NavDrawerContent(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit,
    onCloseDrawer: () -> Unit
) {
    val context = LocalContext.current

    ModalDrawerSheet(
        modifier = Modifier.width(310.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            // Header with Mutiganda Yvan VPN Branding in Umutuku (Red)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VpnRedPrimary)
                    .padding(horizontal = 20.dp, vertical = 22.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_vpn_logo),
                            contentDescription = "Mutiganda Yvan VPN Official Logo",
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Mutiganda Yvan VPN",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "v1.0.0 (Rwanda VIP Edition)",
                                fontSize = 11.5.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "High-Speed Secure Tunnel Engine 🇷🇼",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.95f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            DrawerItem(
                icon = Icons.Default.Home,
                label = "Home Dashboard",
                selected = currentScreen == AppScreen.HOME,
                testTag = "drawer_item_home",
                onClick = {
                    onNavigate(AppScreen.HOME)
                    onCloseDrawer()
                }
            )

            DrawerItem(
                icon = Icons.Default.Code,
                label = "Payload Generator",
                selected = currentScreen == AppScreen.PAYLOAD_GEN,
                testTag = "drawer_item_payload",
                onClick = {
                    onNavigate(AppScreen.PAYLOAD_GEN)
                    onCloseDrawer()
                }
            )

            DrawerItem(
                icon = Icons.Default.Lock,
                label = "Custom Host & SNI",
                selected = currentScreen == AppScreen.HOST_SNI,
                testTag = "drawer_item_sni",
                onClick = {
                    onNavigate(AppScreen.HOST_SNI)
                    onCloseDrawer()
                }
            )

            DrawerItem(
                icon = Icons.Default.Dns,
                label = "DNS Tunnel Configuration",
                selected = currentScreen == AppScreen.DNS_TUNNEL,
                testTag = "drawer_item_dns",
                onClick = {
                    onNavigate(AppScreen.DNS_TUNNEL)
                    onCloseDrawer()
                }
            )

            DrawerItem(
                icon = Icons.Default.Language,
                label = "Server Management",
                selected = currentScreen == AppScreen.SERVERS,
                testTag = "drawer_item_servers",
                onClick = {
                    onNavigate(AppScreen.SERVERS)
                    onCloseDrawer()
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                color = DividerDefaults.color.copy(alpha = 0.4f)
            )

            DrawerItem(
                icon = Icons.Default.Upload,
                label = "Export Config",
                selected = currentScreen == AppScreen.IMPORT_EXPORT,
                testTag = "drawer_item_export",
                onClick = {
                    onNavigate(AppScreen.IMPORT_EXPORT)
                    onCloseDrawer()
                }
            )

            DrawerItem(
                icon = Icons.Default.Download,
                label = "Import Config",
                selected = currentScreen == AppScreen.IMPORT_EXPORT,
                testTag = "drawer_item_import",
                onClick = {
                    onNavigate(AppScreen.IMPORT_EXPORT)
                    onCloseDrawer()
                }
            )

            DrawerItem(
                icon = Icons.Default.Settings,
                label = "Settings",
                selected = currentScreen == AppScreen.SETTINGS,
                testTag = "drawer_item_settings",
                onClick = {
                    onNavigate(AppScreen.SETTINGS)
                    onCloseDrawer()
                }
            )

            DrawerItem(
                icon = Icons.Default.ListAlt,
                label = "Connection Logs",
                selected = currentScreen == AppScreen.LOGS,
                testTag = "drawer_item_logs",
                onClick = {
                    onNavigate(AppScreen.LOGS)
                    onCloseDrawer()
                }
            )

            DrawerItem(
                icon = Icons.Default.Info,
                label = "About Mutiganda Yvan VPN",
                selected = currentScreen == AppScreen.ABOUT,
                testTag = "drawer_item_about",
                onClick = {
                    onNavigate(AppScreen.ABOUT)
                    onCloseDrawer()
                }
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = DividerDefaults.color.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Mutiganda Yvan Developer & WhatsApp Card in Drawer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    .padding(12.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.mutiganda_yvan_photo),
                            contentDescription = "Mutiganda Yvan",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .border(2.dp, VpnRedPrimary, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Mutiganda Yvan 🇷🇼",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Lead Architect & Founder",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Direct WhatsApp Action Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(VpnWhatsAppGreen)
                            .clickable {
                                val url = "https://wa.me/250787699523?text=Hello%20Mutiganda%20Yvan,%20I%20am%20using%20Mutiganda%20Yvan%20VPN!"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            }
                            .padding(vertical = 7.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "WhatsApp",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "WhatsApp: 0787699523",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DrawerItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) VpnRedPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        label = {
            Text(
                text = label,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp
            )
        },
        selected = selected,
        onClick = onClick,
        modifier = Modifier
            .padding(NavigationDrawerItemDefaults.ItemPadding)
            .testTag(testTag),
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = VpnRedPrimary.copy(alpha = 0.12f),
            selectedTextColor = VpnRedPrimary,
            unselectedTextColor = MaterialTheme.colorScheme.onSurface
        )
    )
}
