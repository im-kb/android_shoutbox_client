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
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.ppsm_budzik_shoutbox.*
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
    private lateinit var messagesData: ArrayList<MyMessage>
    private val baseUrl: String = "http://tgryl.pl/"
    private lateinit var currentUsersLogin: String
    private lateinit var jsonPlaceholderAPI: JsonPlaceholderAPI
    private lateinit var retrofit: Retrofit
    private lateinit var enterMessage: EditText
    private lateinit var addMessage: ImageButton
    private lateinit var headerView: View


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

        ///////////////LOGIN
        loadLogin()
        makeToast("login to:" + currentUsersLogin)

        headerView = navView.getHeaderView(0)

        var currentUserTextView: TextView = headerView.findViewById(R.id.currentUserTextView)
        if (currentUserTextView != null) {
            currentUserTextView.text = "Jesteś zalogowany jako: " + currentUsersLogin
        }
        ///////////////

        getAndShowData()
        beginRefreshing()

        addMessage.setOnClickListener {
            val newMessage = MyMessage(currentUsersLogin!!, enterMessage.text.toString())
            if (checkNetworkConnection()) {
                if (enterMessage.text.toString() == null || enterMessage.text.toString() == "") {
                    makeToast("Cannot send empty message")
                } else {
                    sendMessage(newMessage)
                    makeToast("Message sent:" + enterMessage.text.toString())
                    enterMessage.text.clear()
                    getAndShowData()
                }
            } else {
                makeToast("No internet connection")
            }
        }

        var swipeRefresh: SwipeRefreshLayout = root.findViewById(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener {
            if (checkNetworkConnection()) {
                getAndShowData()
                swipeRefresh.isRefreshing = false
                makeToast("Messages refreshed")
            } else {
                makeToast("Cannot refresh messages - no internet connection!")
            }
        }
        return root
    }

    fun getAndShowData() {
        val call = jsonPlaceholderAPI.getMessageArrayList()
        call!!.enqueue(object : Callback<ArrayList<MyMessage>?> {
            override fun onResponse(
                call: Call<ArrayList<MyMessage>?>,
                response: Response<ArrayList<MyMessage>?>
            ) {
                if (!response.isSuccessful) {
                    println("Code: " + response.code())
                    return
                }
                messagesData = response.body()!!
                messagesData.reverse();
                recyclerView.adapter = CustomListAdapter(messagesData, this@ShoutboxFragment)
                recyclerView.layoutManager = LinearLayoutManager(context)

                val swipeHandler = object : SwipeToDeleteCallBack(context) {
                    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                        val adapter = recyclerView.adapter as CustomListAdapter
                        val mess = adapter.getItem(viewHolder.adapterPosition)
                        if (checkNetworkConnection()) {
                            if (currentUsersLogin == mess.login) {
                                mess.id?.let { deleteMessage(it) };
                                adapter.removeAt(viewHolder.adapterPosition)
                                makeToast("Message deleted.")
                            } else {
                                makeToast("This is not your message")
                                getAndShowData()
                            }
                        } else {
                            makeToast("No internet connection.")
                        }
                    }
                }
                val itemTouchHelper = ItemTouchHelper(swipeHandler)
                itemTouchHelper.attachToRecyclerView(recyclerView)
            }

            override fun onFailure(
                call: Call<ArrayList<MyMessage>?>,
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

    private fun loadLogin() {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        val defaultValue = resources.getString(R.string.saved_login)
        currentUsersLogin =
            sharedPref.getString(getString(R.string.saved_login), defaultValue).toString()
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

    fun beginRefreshing() {
        thread.scheduleAtFixedRate({
            if (checkNetworkConnection()) {
                getAndShowData()
                Log.d("Executors thread: ", "Messages refreshed automatically ")
            } else {
                Log.d(
                    "Executors thread: ",
                    "Cant automatically refresh messages - no internet connection!"
                )
            }
        }, 0, 30, TimeUnit.SECONDS)
    }

    override fun onItemClick(//dzialanie edycji - klikniecia na cokolwiek z listy wiadomosci
        item: MyMessage, position: Int
    ) {
        if (currentUsersLogin == item.login) {
            val bundle = Bundle()
            bundle.putString("login", item.login)
            bundle.putString("id", item.id)
            bundle.putString("date_hour", item.date)
            bundle.putString("content", item.content)
            val fragment: Fragment = EditFragment()
            fragment.arguments = bundle
            val fragmentManager: FragmentManager? = fragmentManager
            fragmentManager?.beginTransaction()
                ?.add(R.id.nav_host_fragment, fragment)
                ?.addToBackStack(this.toString())
               // ?.remove(this)
                ?.commit()
        } else {
            makeToast("You can only edit your own messages!!!")
        }
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

    private fun deleteMessage(id: String) {
        val call = jsonPlaceholderAPI.createDelete(id)
        call.enqueue(object : Callback<MyMessage> {
            override fun onResponse(
                call: Call<MyMessage>,
                response: Response<MyMessage>
            ) {
                if (!response.isSuccessful) {
                    println("Code: " + response.code())
                    return
                }
            }

            override fun onFailure(
                call: Call<MyMessage>,
                t: Throwable
            ) {
                println(t.message)
            }
        })
    }
}