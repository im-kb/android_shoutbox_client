package com.example.ppsm_budzik_shoutbox.ui.shoutbox

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.ppsm_budzik_shoutbox.JsonPlaceholderAPI
import com.example.ppsm_budzik_shoutbox.CustomListAdapter
import com.example.ppsm_budzik_shoutbox.MyMessage
import com.example.ppsm_budzik_shoutbox.R
import kotlinx.android.synthetic.main.fragment_shoutbox.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ShoutboxFragment : Fragment(), CustomListAdapter.OnItemClickListener {

    private lateinit var shoutboxViewModel: ShoutboxViewModel
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var infoToast: Toast
    private lateinit var messagesData: Array<MyMessage>
    private val baseUrl: String = "http://tgryl.pl/"
    private lateinit var login: String
    private lateinit var jsonPlaceholderAPI: JsonPlaceholderAPI
    private lateinit var retrofit: Retrofit
    private lateinit var enterMessage: EditText
    private lateinit var addMessage: ImageButton
    private lateinit var xlogin: String

    val thread = Executors.newSingleThreadScheduledExecutor()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        shoutboxViewModel =
            ViewModelProviders.of(this).get(ShoutboxViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_shoutbox, container, false)

        enterMessage = root.findViewById(R.id.enterMessage)
        addMessage = root.findViewById(R.id.addMessage)

        //////////json
        retrofit = Retrofit.Builder().baseUrl(baseUrl)
            .addConverterFactory(
                GsonConverterFactory
                    .create()
            )
            .build()
        jsonPlaceholderAPI = retrofit.create(JsonPlaceholderAPI::class.java)
        ////json
        login = arguments?.getString("login").toString()
        getAndShowData(jsonPlaceholderAPI)
        beginRefreshing()

        addMessage.setOnClickListener {
            val newMessage = MyMessage(login!!, enterMessage.text.toString())
            if (checkNetworkConnection()) {
                if (enterMessage.text.toString() == null || enterMessage.text.toString() == "") {
                    makeToast("Cannot send empty message")
                } else {
                    sendMessage(newMessage)
                    makeToast("Message sent:" + enterMessage.text.toString())
                    getAndShowData(jsonPlaceholderAPI)
                    enterMessage.text.clear()
                }
            } else {
                makeToast("No internet connection")
            }
        }

        var swipeRefresh: SwipeRefreshLayout = root.findViewById(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener {
            if (checkNetworkConnection()) {
                getAndShowData(jsonPlaceholderAPI)
                swipeRefresh.isRefreshing = false
                makeToast("Messages refreshed")
            } else {
                makeToast("Cannot refresh messages - no internet connection!")
            }
        }
        return root
    }

    fun updateRecyclerView() {
        if (recyclerView != null) {
            recyclerView.adapter = CustomListAdapter(messagesData, this)
            recyclerView.layoutManager = LinearLayoutManager(context)
        }
    }

    fun getAndShowData(jsonPlaceholderAPI: JsonPlaceholderAPI) {
        val call = jsonPlaceholderAPI.getMessageArray()
        call!!.enqueue(object : Callback<Array<MyMessage>?> {
            override fun onResponse(
                call: Call<Array<MyMessage>?>,
                response: Response<Array<MyMessage>?>
            ) {
                if (!response.isSuccessful) {
                    println("Code: " + response.code())
                    return
                }
                messagesData = response.body()!!
                messagesData.reverse();
                updateRecyclerView()
            }

            override fun onFailure(
                call: Call<Array<MyMessage>?>,
                t: Throwable
            ) {
                println(t.message)
            }
        })
    }

    fun checkNetworkConnection(): Boolean {
        val connectivityManager =
            context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager != null) {
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                    return true
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                    return true
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                    return true
                }
            }
        }
        return false
    }

    fun makeToast(myToastText: String) {
        infoToast = Toast.makeText(
            context,
            myToastText,
            Toast.LENGTH_SHORT
        )
        infoToast.setGravity(Gravity.TOP, 0, 200)
        infoToast.show()
    }

    override fun onItemClick(//dzialanie edycji - klikniecia na cokolwiek z listy wiadomosci
        item: MyMessage, position: Int
    ) {
        val prefs =
            requireActivity().getPreferences(Context.MODE_PRIVATE)
        xlogin = prefs.getString("login", "").toString();
        if (xlogin == item.login) {
            val bundle = Bundle()
            bundle.putString("login", item.login)
            bundle.putString("id", item.id)
            bundle.putString("date_hour", item.date)
            bundle.putString("content", item.content)
            val fragment: Fragment = EditFragment()
            fragment.arguments = bundle
            val fragmentManager: FragmentManager? = fragmentManager
            fragmentManager?.beginTransaction()
                ?.replace(R.id.nav_host_fragment, fragment)
                ?.remove(this)
                ?.commit()
        } else {
            makeToast("You can only edit your own messages!!!")
        }
    }

    fun beginRefreshing() {
        thread.scheduleAtFixedRate({
            if (checkNetworkConnection()) {
                getAndShowData(jsonPlaceholderAPI)
                Log.d("Executors thread: ", "Messages refreshed automatically ")
            } else {
                Log.d(
                    "Executors thread: ",
                    "Cant automatically refresh messages - no internet connection!"
                )
            }
        }, 0, 30, TimeUnit.SECONDS)
    }

    private fun sendMessage(MyMessage: MyMessage) {
        val call = jsonPlaceholderAPI.createPost(MyMessage)
        call.enqueue(object : Callback<MyMessage> {
            override fun onFailure(
                call: Call<MyMessage>,
                t: Throwable
            ) {
                makeToast("Can't send message")
            }

            override fun onResponse(
                call: Call<MyMessage>,
                response: Response<MyMessage>
            ) {
                if (!response.isSuccessful) {
                    println("Code: " + response.code())
                    return
                }
            }
        })
    }
}