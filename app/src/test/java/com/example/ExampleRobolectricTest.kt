package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.VpnRepository
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
  fun `verify vpn repository initialization and default servers`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = VpnRepository.getInstance(context)
    val servers = repository.servers.value
    assertTrue("Servers list should not be empty", servers.isNotEmpty())
    assertNotNull(servers.find { it.id == "random" })
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
}
