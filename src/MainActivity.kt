package hello.world

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.utsman.kece.BuildConfig
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

@Composable
fun Screen() {
    val viewModel: MainViewModel = viewModel()
    val state by viewModel.renderState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.getProduct()
    }

    MaterialTheme {
        Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (val value = state) {
                is RenderState.RenderLoading -> {
                    CircularProgressIndicator()
                }

                is RenderState.RenderProduct -> {
                    BasicText(
                            text = value.product
                    )
                }

                is RenderState.RenderFailure -> {
                    BasicText(
                            text = value.throwable.localizedMessage.orEmpty(),
                            color = ColorProducer {
                                Color.Red
                            }
                    )
                }

                else -> {}
            }
        }
    }
}

class MainActivity : AppCompatActivity() {

    private val versionName by lazy {
        BuildConfig.VERSION_NAME
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    text = "version: v$versionName"
                )
            }
        }
    }
}