package hello.world

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.viewModelFactory
import com.utsman.kece.R
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface RenderState {
    object Idle : RenderState
    object RenderLoading : RenderState
    data class RenderProduct(val product: String) : RenderState
    data class RenderFailure(val throwable: Throwable) : RenderState
}

object WebService {
    private val client = HttpClient(OkHttp)
    
    suspend fun getProductFlow(): Flow<RenderState> {

        return flow {
            val httpResponse = client.get("https://marketfake.fly.dev/product")
            if (httpResponse.status.isSuccess()) {
                emit(RenderState.RenderProduct(httpResponse.bodyAsText()))
            } else {
                emit(RenderState.RenderFailure(Throwable(httpResponse.bodyAsText())))
            }
        }.catch {
            emit(RenderState.RenderFailure(it))
        }.onStart {
            emit(RenderState.RenderLoading)
        }
    }
}

class MainViewModel : ViewModel() {
    private val _renderState: MutableStateFlow<RenderState> = MutableStateFlow(RenderState.Idle)
    val renderState: StateFlow<RenderState> = _renderState.asStateFlow()
    
    fun getProduct() = viewModelScope.launch {
        WebService
            .getProductFlow()
            .stateIn(this)
            .collect(_renderState)
    }
}

class MainActivity : AppCompatActivity() {

    val viewModel: MainViewModel by lazy {
        viewModelFactory {  }
            .create(MainViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lifecycleScope.launch {
            viewModel.renderState.collectLatest { renderState ->
                when (renderState) {
                    is RenderState.RenderProduct -> {
                        findViewById<TextView>(R.id.activity_main)
                                .text = renderState.product
                    }
                    else -> {
                        findViewById<TextView>(R.id.activity_main)
                                .text = renderState.javaClass.simpleName
                    }
                }
            }
        }
    }
}