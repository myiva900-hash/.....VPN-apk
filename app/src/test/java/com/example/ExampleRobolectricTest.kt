package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.VpnRepository
import com.example.data.local.VpnDatabase
import com.example.model.ConnectionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Mutiganda Yvan VPN", appName)
  }

  @Test
  fun `verify vpn repository initialization and default servers including Germany Frankfurt`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = VpnRepository.getInstance(context)
    val servers = repository.servers.value
    assertTrue("Servers list should not be empty", servers.isNotEmpty())
    
    val deServer = servers.find { it.id == "de_fra" }
    assertNotNull("Germany Frankfurt server must exist", deServer)
    assertEquals("de-fra.vpn.mutigandayvan.net", deServer?.ip)

    val rwServer = servers.find { it.id == "rw_gisozi" }
    assertNotNull("Rwanda Gisozi VIP server must exist", rwServer)
  }

  @Test
  fun `verify connection modes support`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = VpnRepository.getInstance(context)
    repository.setConnectionMode(ConnectionMode.CUSTOM_SNI)
    assertEquals(ConnectionMode.CUSTOM_SNI, repository.config.value.connectionMode)

    repository.setConnectionMode(ConnectionMode.DNS_TUNNEL)
    assertEquals(ConnectionMode.DNS_TUNNEL, repository.config.value.connectionMode)
  }

  @Test
  fun `verify connection duration clamping prevents unusually large values`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = VpnRepository.getInstance(context)
    
    // Normal duration
    repository.updateDuration(120L)
    assertEquals(120L, repository.connectionDurationSeconds.value)

    // Unusually large corrupted value (e.g. 1.7 billion seconds)
    repository.updateDuration(1794248800L)
    assertEquals(0L, repository.connectionDurationSeconds.value)

    // Negative value
    repository.updateDuration(-50L)
    assertEquals(0L, repository.connectionDurationSeconds.value)
  }

  @Test
  fun `verify room database instantiation and custom server profile dao`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = VpnDatabase.getInstance(context)
    assertNotNull(db.serverValidationDao())
    assertNotNull(db.customServerProfileDao())
  }

  @Test
  fun `verify custom server profile conversion and keystore encryption roundtrip`() {
    val rawPassword = "SuperSecretPassword123!"
    val encrypted = com.example.data.security.KeystoreEncryptionHelper.encrypt(rawPassword)
    assertTrue("Encrypted string should not be blank", encrypted.isNotBlank())
    assertTrue("Encrypted string must not contain plain password", !encrypted.contains(rawPassword))

    val decrypted = com.example.data.security.KeystoreEncryptionHelper.decrypt(encrypted)
    assertEquals("Decrypted password must match original", rawPassword, decrypted)

    val profile = com.example.data.local.CustomServerProfile(
      id = "custom_test_1",
      serverName = "Dedicated Frankfurt Node",
      hostOrIp = "198.51.100.42",
      port = 1194,
      username = "test_user",
      encryptedPassword = encrypted,
      vpnProtocol = "OpenVPN",
      location = "Frankfurt, Germany",
      countryCode = "DE",
      flag = "🇩🇪"
    )

    val vpnServer = profile.toVpnServer()
    assertEquals("custom_test_1", vpnServer.id)
    assertEquals("Dedicated Frankfurt Node", vpnServer.name)
    assertEquals("198.51.100.42", vpnServer.ip)
    assertTrue(vpnServer.isCustom)
    assertTrue(vpnServer.supportedPorts.contains(1194))
  }
}
