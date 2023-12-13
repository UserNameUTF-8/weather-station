package org.recherche.ms_karim_weather_project

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.components.YAxis.AxisDependency
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import info.mqtt.android.service.MqttAndroidClient
import info.mqtt.android.service.MqttService
import info.mqtt.android.service.MqttTraceHandler
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.w3c.dom.Entity
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import kotlin.math.log
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class MainActivity : AppCompatActivity() {
    lateinit var lineChart: LineChart
    var new_x = 4;
    val CLIENT_ID = "client"
    val BROKER_URL = "YOUR BROKER URL"
    val USERNAME = "YOUR BROKER USERNAME"
    val PASSWORD = "YOUR BROKER PASSWORD"

    lateinit var presure: TextView

    lateinit var mqttAndroidClient: MqttAndroidClient
    val topicDHT11 = "/temp/dht11"
    val topicDHT22 = "/temp/dht22"
    val topicBME280 = "/temp/bme280"
    val topicHumidityDHT11 = "/hum/dht11"
    val topicHumidityDHT22 = "/hum/dht22"
    val topicDISCONNECT = "/disconnect"
    var comeFromDHT11 = false
    var comeFromDHT22 = false
    lateinit var vl: LineDataSet
    lateinit var vl2: LineDataSet
    lateinit var vlBmp: LineDataSet
    lateinit var btnSync: ImageButton
    lateinit var statusImage: ImageView
    lateinit var params_: MqttConnectOptions
    lateinit var barChart: BarChart
    lateinit var barDataSetDHT11: BarDataSet
    lateinit var barDataSetDHT22: BarDataSet
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lineChart = findViewById(R.id.line_chart)
        btnSync = findViewById(R.id.sync)
        statusImage = findViewById(R.id.img_status)
        barChart = findViewById(R.id.bar_data_set)

        btnSync.isEnabled = false
        val description = Description()
        description.text = "Temperature"
        lineChart.description = description

        """
            Set Up Mqtt client
        """.trimIndent()





        mqttAndroidClient = MqttAndroidClient(this, BROKER_URL, CLIENT_ID)
        // setup certification

        val cf = CertificateFactory.getInstance("X.509")
        val cert = applicationContext.resources?.openRawResource(R.raw.server1)

        val ca = cf.generateCertificate(cert)
        val keyStoreType = KeyStore.getDefaultType()
        val keyStore = KeyStore.getInstance(keyStoreType)
        keyStore.load(null, null)
        keyStore.setCertificateEntry("ca", ca)

        val tmfAlgo = TrustManagerFactory.getDefaultAlgorithm()
        val tmf = TrustManagerFactory.getInstance(tmfAlgo)
        tmf.init(keyStore)

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, tmf.trustManagers, null)

        //sslContext.socketFactory

        params_ = MqttConnectOptions()
        params_.userName = USERNAME
        params_.password = PASSWORD.toCharArray()
        params_.socketFactory = sslContext.socketFactory

        val token = mqttAndroidClient.connect(params_)
        token.actionCallback = tokenActionListener
        mqttAndroidClient.addCallback(clientCallbacks)

        presure = findViewById(R.id.id_presure)

        presure.setText("000")
        var entries = ArrayList<Entry>()
        entries.add(Entry(0f, 0f))


        val entriesHumidity = ArrayList<Entry>()
        entriesHumidity.add(Entry(0f, 0f)) // Add some initial data for humidity


        val entriesBME = ArrayList<Entry>()
        entriesBME.add(Entry(0f, 0f)) //

        lineChart.xAxis.valueFormatter = object : IndexAxisValueFormatter() {
            private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            override fun getFormattedValue(value: Float, axis: AxisBase?): String {

                return ""
            }
        }



        vl = LineDataSet(entries, "DHT11") // dataset
        vl2 = LineDataSet(entriesHumidity, "DHT22")
        vlBmp = LineDataSet(entriesBME, "BME")
        val line = LineData(vl, vl2, vlBmp)
        vl2.color = 0xff000000.toInt()
        vlBmp.color = 0xfff18770.toInt()

//        addButton.setOnClickListener {
//            lineChart.setVisibleXRangeMaximum(10f)
//
//            var int_ = kotlin.random.Random.nextInt(1, 30 + 1)
//            var int_2 = kotlin.random.Random.nextInt(1, 30 + 1)
//
//            val current_time = System.currentTimeMillis()
//
//            vl.addEntry(Entry(new_x.toFloat(), int_.toFloat()))
//            vl2.addEntry(Entry(new_x.toFloat(), int_2.toFloat()))
//            new_x += 1
//            lineChart.data.notifyDataChanged()
//            lineChart.notifyDataSetChanged()
//            lineChart.moveViewToX(new_x.toFloat() - 10)
//            lineChart.invalidate()
//
//        }


        lineChart.setTouchEnabled(true)
        lineChart.setPinchZoom(true)
        lineChart.isScrollContainer = true

        lineChart.data = line

        val listDHt11 = ArrayList<BarEntry>()
        var date_ = BarEntry(0f, 0f)

        listDHt11.add(date_)

        val listDHT22 = ArrayList<BarEntry>()
        listDHT22.add(BarEntry(1f, 10f))

        barDataSetDHT11 = BarDataSet(listDHt11, "DHT11")
        barDataSetDHT22 = BarDataSet(listDHT22, "DHT22")
        barDataSetDHT11.color = 0xffd1cfd7.toInt()

        var barData = BarData(barDataSetDHT11, barDataSetDHT22)
        barChart.data = barData
        barChart.axisLeft.axisMinimum = 0f
        barChart.axisLeft.axisMaximum = 100f

        var desc = Description()
        desc.text = "Humidity"
        barChart.description = desc


    }


    private val clientCallbacks = object : MqttCallback {
        override fun connectionLost(cause: Throwable?) {
            statusImage.setImageResource(R.drawable.baseline_fiber_manual_record_24_offline)
            btnSync.isEnabled = true
            Toast.makeText(
                this@MainActivity,
                "failed to connect cause ${cause!!.message}",
                Toast.LENGTH_SHORT
            ).show()

        }

        override fun messageArrived(topic: String?, message: MqttMessage?) {

//            Log.d("TAG", "messageArrived: $message")
            lineChart.setVisibleXRangeMaximum(5f)
            if (topic == topicDHT11) {
                vl.addEntry(Entry(new_x.toFloat(), message.toString().toFloat()))
                comeFromDHT11 = true
            } else if (topic == topicDHT22) {
                vl2.addEntry(Entry(new_x.toFloat(), message.toString().toFloat()))
                comeFromDHT22 = true
            } else if (topic == topicHumidityDHT22) {
                barChart.data.getDataSetByIndex(1).getEntryForIndex(0).y =
                    message.toString().toFloat()
                barChart.data.notifyDataChanged()
                barChart.notifyDataSetChanged()
                barChart.invalidate()

            } else if (topic == topicHumidityDHT11 && message != null && message.toString() != "") {
                barChart.data.getDataSetByIndex(0).getEntryForIndex(0).y =
                    message.toString().toFloat()
                barChart.data.notifyDataChanged()
                barChart.notifyDataSetChanged()
                barChart.invalidate()
            }

            if (topic == topicDISCONNECT) {
                Toast.makeText(this@MainActivity, "Sensors Stop Emitting data", Toast.LENGTH_LONG)
                    .show()
                Log.d("TAG", "messageArrived: im in callback disconnect")
                statusImage.setImageResource(R.drawable.baseline_fiber_manual_record_24_nodata)

            }

            if (topic == "/info/temp") {
                val strings = message.toString().split("_T_")
                Log.d("TAG", "messageArrived: $strings ")
                vl.addEntry(Entry(new_x.toFloat(), strings[0].toFloat()))
                vl2.addEntry(Entry(new_x.toFloat(), strings[1].toFloat()))
                vlBmp.addEntry(Entry(new_x.toFloat(), strings[2].toFloat()))

                Log.d("TAG", "messageArrived: ${strings[2]} ")
                lineChart.data.notifyDataChanged()
                lineChart.notifyDataSetChanged()
                lineChart.moveViewToX(new_x.toFloat() - 5)

                new_x += 1

                lineChart.invalidate()
                comeFromDHT11 = false
                comeFromDHT22 = false

            }


            if (topic == topicDHT22 || topic == topicDHT11)
                if (comeFromDHT11 && comeFromDHT22) {
                    lineChart.data.notifyDataChanged()
                    lineChart.notifyDataSetChanged()
                    lineChart.moveViewToX(new_x.toFloat() - 5)

                    new_x += 1

                    lineChart.invalidate()
                    comeFromDHT11 = false
                    comeFromDHT22 = false
                }

            if (topic == "/info/pre") {
                Log.d("TAG", "messageArrived: $message ")
                presure.setText(message.toString())
            }


        }

        override fun deliveryComplete(token: IMqttDeliveryToken?) {
            Log.d("TAG", "deliveryComplete: token")
        }

    }


    private val tokenActionListener = object : IMqttActionListener {
        override fun onSuccess(asyncActionToken: IMqttToken?) {
            mqttAndroidClient.subscribe("/info/#", 1).actionCallback =
                object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.d("TAG", "onSuccess: subscribe success")
                        statusImage.setImageResource(R.drawable.baseline_fiber_manual_record_24)
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        exception!!.printStackTrace()
                        btnSync.isEnabled = true

                    }

                }

            mqttAndroidClient.subscribe(topicHumidityDHT11, 1).actionCallback =
                object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.d("TAG", "onSuccess: humidity saved")
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.d("TAG", "onFailure: failed cause of ${exception!!.message}")
                    }

                }
            mqttAndroidClient.subscribe(topicDISCONNECT, 1).actionCallback =
                object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.d("TAG", "onSuccess: humidity saved")
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.d("TAG", "onFailure: failed cause of ${exception!!.message}")
                    }

                }

            mqttAndroidClient.subscribe(topicHumidityDHT22, 1).actionCallback =
                object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.d("TAG", "onSuccess: humidity saved")
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.d("TAG", "onFailure: failed cause of ${exception!!.message}")
                    }

                }


        }

        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
            Log.d("TAG", "onFailure:  failed ")
            exception!!.printStackTrace()
//            Toast.makeText(
//                this@MainActivity,
//                "failed to connect cause ${exception.message}",
//                Toast.LENGTH_SHORT
//            ).show()
        }

    }


    val syncClickListener = OnClickListener {
        mqttAndroidClient.connect(params_).actionCallback = object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                statusImage.setImageResource(R.drawable.baseline_fiber_manual_record_24)
                Toast.makeText(this@MainActivity, "connected to broker", Toast.LENGTH_SHORT).show()
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {

                Toast.makeText(this@MainActivity, "connected to broker", Toast.LENGTH_SHORT).show()
                statusImage.setImageResource(R.drawable.baseline_fiber_manual_record_24_offline)
                Toast.makeText(
                    this@MainActivity,
                    "failed to connect cause ${exception!!.message}",
                    Toast.LENGTH_SHORT
                ).show()


            }

        }
    }


}