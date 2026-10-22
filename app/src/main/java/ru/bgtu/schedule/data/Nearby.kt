package ru.bgtu.schedule.data

import android.Manifest
import android.content.Context
import android.os.Build
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.util.UUID

data class NearbyMessage(val id: String, val author: String, val text: String, val at: Long, val mine: Boolean)

data class NearbyState(
    val running: Boolean = false,
    val peers: Map<String, String> = emptyMap(),
    val messages: List<NearbyMessage> = emptyList(),
    val error: String? = null,
)

/**
 * Chat with phones around without internet (Bluetooth and Wi-Fi via Google Nearby Connections).
 * Messages are relayed through connected phones a few hops further and are not saved anywhere.
 */
object NearbyChat {
    const val MAX_TEXT = 500
    private const val SERVICE = "ru.bgtu.schedule.nearby"
    private const val HOPS = 3
    private const val KEEP = 200

    private val flow = MutableStateFlow(NearbyState())
    val state: StateFlow<NearbyState> = flow.asStateFlow()

    private var client: ConnectionsClient? = null
    private var myName = ""
    private val names = mutableMapOf<String, String>()
    private val requested = mutableSetOf<String>()
    private val seen = LinkedHashSet<String>()

    fun permissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= 33 -> arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.NEARBY_WIFI_DEVICES,
        )
        Build.VERSION.SDK_INT >= 31 -> arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
        Build.VERSION.SDK_INT >= 29 -> arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        else -> arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    private val payloads = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val bytes = payload.asBytes() ?: return
            receive(endpointId, bytes)
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) = Unit
    }

    private val connections = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            names[endpointId] = info.endpointName
            client?.acceptConnection(endpointId, payloads)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            requested -= endpointId
            if (result.status.isSuccess) {
                flow.value = flow.value.copy(peers = flow.value.peers + (endpointId to (names[endpointId] ?: "?")))
            }
        }

        override fun onDisconnected(endpointId: String) {
            requested -= endpointId
            flow.value = flow.value.copy(peers = flow.value.peers - endpointId)
        }
    }

    private val discovery = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            if (info.serviceId != SERVICE || endpointId in flow.value.peers || endpointId in requested) return
            requested += endpointId
            names[endpointId] = info.endpointName
            client?.requestConnection(myName, endpointId, connections)?.addOnFailureListener { requested -= endpointId }
        }

        override fun onEndpointLost(endpointId: String) {
            requested -= endpointId
        }
    }

    fun start(context: Context, name: String) {
        if (flow.value.running) return
        myName = name.trim().ifBlank { "Студент" }.take(40)
        val connectionsClient = Nearby.getConnectionsClient(context.applicationContext)
        client = connectionsClient
        flow.value = flow.value.copy(running = true, error = null)
        val strategy = Strategy.P2P_CLUSTER
        connectionsClient.startAdvertising(myName, SERVICE, connections, AdvertisingOptions.Builder().setStrategy(strategy).build())
            .addOnFailureListener { fail(it) }
        connectionsClient.startDiscovery(SERVICE, discovery, DiscoveryOptions.Builder().setStrategy(strategy).build())
            .addOnFailureListener { fail(it) }
    }

    private fun fail(error: Exception) {
        stop()
        flow.value = flow.value.copy(error = error.message ?: "error")
    }

    fun stop() {
        client?.apply {
            stopAdvertising()
            stopDiscovery()
            stopAllEndpoints()
        }
        client = null
        requested.clear()
        flow.value = flow.value.copy(running = false, peers = emptyMap())
    }

    fun send(text: String) {
        val body = text.trim().take(MAX_TEXT)
        if (body.isEmpty()) return
        val message = NearbyMessage(UUID.randomUUID().toString(), myName, body, System.currentTimeMillis(), mine = true)
        remember(message)
        broadcast(message, HOPS, except = null)
    }

    private fun receive(from: String, bytes: ByteArray) {
        val json = runCatching { JSONObject(String(bytes, Charsets.UTF_8)) }.getOrNull() ?: return
        val id = json.optString("id")
        val text = json.optString("text").trim().take(MAX_TEXT)
        if (id.isBlank() || text.isBlank() || id in seen) return
        val message = NearbyMessage(id, json.optString("author").take(40).ifBlank { "?" }, text, json.optLong("at"), mine = false)
        remember(message)
        val hops = json.optInt("hops", 0) - 1
        if (hops > 0) broadcast(message, hops, except = from)
    }

    private fun remember(message: NearbyMessage) {
        seen += message.id
        if (seen.size > 1000) seen.remove(seen.first())
        flow.value = flow.value.copy(messages = (flow.value.messages + message).takeLast(KEEP))
    }

    private fun broadcast(message: NearbyMessage, hops: Int, except: String?) {
        val targets = flow.value.peers.keys.filter { it != except }
        if (targets.isEmpty()) return
        val payload = JSONObject()
            .put("id", message.id)
            .put("author", message.author)
            .put("text", message.text)
            .put("at", message.at)
            .put("hops", hops)
        client?.sendPayload(targets, Payload.fromBytes(payload.toString().toByteArray(Charsets.UTF_8)))
    }

    fun clear() {
        flow.value = flow.value.copy(messages = emptyList())
    }
}
