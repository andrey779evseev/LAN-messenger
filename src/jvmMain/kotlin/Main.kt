// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.io.IOException
import java.net.*
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*


fun getLocalIpAddress(): String? {
    try {
        val en: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
        while (en.hasMoreElements()) {
            val intf: NetworkInterface = en.nextElement()
            val enumIpAddr: Enumeration<InetAddress> = intf.getInetAddresses()
            while (enumIpAddr.hasMoreElements()) {
                val inetAddress = enumIpAddr.nextElement()
                if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                    return inetAddress.getHostAddress()
                }
            }
        }
    } catch (ex: SocketException) {
        ex.printStackTrace()
    }
    return null
}

class udp_DataArrival: Runnable {
    override fun run() {
        while (true){
            receiveUDP()
        }
    }
    open fun receiveUDP() {
        val buffer = ByteArray(2048)
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket(1111, InetAddress.getByName(getLocalIpAddress()))
            socket.broadcast = true
            val packet = DatagramPacket(buffer, buffer.size)
            socket.receive(packet)
            messagesList.add("${SimpleDateFormat("HH:mm:ss").format(Date())} - ${packet.address.toString().replace("/", "")} - ${packet.data.decodeToString().substring(0, packet.data.decodeToString().length - 1)}")
        } catch (e: Exception) {
            println("open fun receiveUDP catch exception." + e.toString())
            e.printStackTrace()
        } finally {
            socket?.close()
        }
    }

}

fun sendMessage(message: String, ip: String) {
    try {
        val socket = DatagramSocket()
        socket.broadcast = true
        val sendData = message.toByteArray()
        val sendPacket = DatagramPacket(sendData, sendData.size, InetAddress.getByName(ip), 1111)
        messagesList.add("${SimpleDateFormat("HH:mm:ss").format(Date())} - you - $message")
        socket.send(sendPacket)
    } catch (e: IOException) {
        println("error : $e")
    }
}


var messagesList = mutableStateListOf<String>()

@Composable
@Preview
fun App(messages: MutableList<String>) {
    val ip = remember { mutableStateOf("") }
    val message = remember { mutableStateOf("") }

    SideEffect {
        val threadWithRunnable = Thread(udp_DataArrival())
        threadWithRunnable.start()
    }

    MaterialTheme {
        Row (modifier = Modifier.padding(all = 10.dp)){
            Column(modifier = Modifier.padding(all = 10.dp)) {
                Text("Your ip ${getLocalIpAddress()}")
                Text("IP")
                TextField(
                    value = ip.value,
                    singleLine = true,
                    onValueChange = { ip.value = it },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
                )
                Text("Message")
                TextField(
                    value = message.value,
                    singleLine = true,
                    onValueChange = { message.value = it },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
                )
                Button(
                    onClick = {
                        sendMessage(message=message.value, ip=ip.value)
                    },
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        top = 8.dp,
                        end = 20.dp,
                        bottom = 8.dp
                    )
                ) {
                    Text("Send")
                }
                Text("Received messages")
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                    modifier = Modifier
                        .padding(all = 10.dp)
                        .border(width=1.dp, color=Color(0xFF33ccff), shape=RoundedCornerShape(8.dp))
                        .padding(all=5.0.dp)
                ) {
                    items(messages) { message ->
                        Text(message)
                    }
                }
            }
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App(messagesList)
    }
}
