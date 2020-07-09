package com.example.androidrosbridge

import java.io.IOException
import java.net.Socket
import java.util.*
/* Copyright (C) 2020 Jonathan Sanabria

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
class RosBridgeClient(private val host: String , private val port: Int) {

    private var socket: Socket? = null
    private var peerName: String? = null
    fun run() {
        Thread() {
            socket = Socket(host.toString(), this.port.toInt())
            socket!!.outputStream.write("Connection Established".toByteArray())
        }.start()

    }

    fun send( operation: String? , data: String?, isMessage: Boolean) = Thread() {
        if( this.socket != null ){
            if (isMessage) {
                val c = Calendar.getInstance().time
                var mymsg = ""
                if( operation.equals("advertise")){
                    mymsg = "{ \"op\": \"" + operation + "\"," +
                            "  \"topic\":  \"/my_published_data\"," +
                            "  \"type\":  \"geometry_msgs/PoseStamped\"" +
                            "}"
                    // "  \"type\":  \"std_msgs/Float32MultiArray\"" +
                    // "  \"type\":  \"std_msgs/String\"" +
                } else {
                    mymsg = "{ \"op\": \"" + operation + "\"," +
                            "  \"topic\":  \"/my_published_data\"," +
                            "  \"msg\":  "+ data +
                            "}"
                    //"  \"msg\":  {\"data\": " + data + " }"+

                }
                socket!!.outputStream.write(mymsg.toByteArray())

            }
        }

    }.start()

    fun DestroySocket() {
        if (socket != null) {
            try {
                socket!!.close()
                socket = null
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }


}