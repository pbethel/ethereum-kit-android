package io.horizontalsystems.oneinchkit

import com.google.gson.annotations.SerializedName
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.oneinchkit.contracts.OneInchContractMethodFactories
import io.horizontalsystems.oneinchkit.decorations.OneInchTransactionDecorator
import io.reactivex.Single
import java.math.BigInteger
import java.util.*

class OneInchKit(
        private val evmKit: EthereumKit,
        private val service: OneInchService
) {

    val getRouterAddress: Address = when (evmKit.chain) {
        Chain.Ethereum, Chain.BinanceSmartChain, Chain.Polygon, Chain.ArbitrumOne -> Address("0x1111111254fb6c44bac0bed2854e76f90643097d")
        Chain.Optimism -> Address("0x1111111254760f7ab3f16433eea9304126dcd199")
        Chain.EthereumRopsten, Chain.EthereumKovan, Chain.EthereumRinkeby, Chain.EthereumGoerli -> Address("0x11111112542d85b3ef69ae05771c2dccff4faa26")
        else -> throw IllegalArgumentException("Invalid Chain: ${evmKit.chain.id}")
    }

    fun getApproveCallDataAsync(tokenAddress: Address, amount: BigInteger): Single<ApproveCallData> {
        return service.getApproveCallDataAsync(tokenAddress, amount)
    }

    fun getQuoteAsync(
            fromToken: Address,
            toToken: Address,
            amount: BigInteger,
            protocols: List<String>? = null,
            gasPrice: GasPrice? = null,
            complexityLevel: Int? = null,
            connectorTokens: List<String>? = null,
            gasLimit: Long? = null,
            mainRouteParts: Int? = null,
            parts: Int? = null
    ): Single<Quote> {
        return service.getQuoteAsync(fromToken, toToken, amount, protocols, gasPrice, complexityLevel, connectorTokens, gasLimit, mainRouteParts, parts)
    }

    fun getSwapAsync(
            fromToken: Address,
            toToken: Address,
            amount: BigInteger,
            slippagePercentage: Float,
            protocols: List<String>? = null,
            recipient: Address? = null,
            gasPrice: GasPrice? = null,
            burnChi: Boolean = false,
            complexityLevel: Int? = null,
            connectorTokens: List<String>? = null,
            allowPartialFill: Boolean = false,
            gasLimit: Long? = null,
            parts: Int? = null,
            mainRouteParts: Int? = null
    ): Single<Swap> {
        return service.getSwapAsync(fromToken, toToken, amount, evmKit.receiveAddress, slippagePercentage, protocols, recipient, gasPrice, burnChi, complexityLevel, connectorTokens, allowPartialFill, gasLimit, parts, mainRouteParts)
    }

    companion object {

        fun getInstance(evmKit: EthereumKit): OneInchKit {
            val service = OneInchService(evmKit.chain)
            return OneInchKit(evmKit, service)
        }

        fun addDecorator(evmKit: EthereumKit) {
            val decorator = OneInchTransactionDecorator(evmKit.receiveAddress, OneInchContractMethodFactories)
            evmKit.addDecorator(decorator)
        }

        fun addTransactionWatcher(evmKit: EthereumKit) {
            val watcher = OneInchTransactionWatcher(evmKit.receiveAddress)
            evmKit.addTransactionWatcher(watcher)
        }

    }

}

data class Token(
        val symbol: String,
        val name: String,
        val decimals: Int,
        val address: String,
        val logoURI: String
)

data class Quote(
        val fromToken: Token,
        val toToken: Token,
        val fromTokenAmount: BigInteger,
        val toTokenAmount: BigInteger,
        @SerializedName("protocols") val route: List<Any>,
        val estimatedGas: Long
) {
    override fun toString(): String {
        return "Quote {fromToken: ${fromToken.name}, toToken: ${toToken.name}, fromTokenAmount: $fromTokenAmount, toTokenAmount: $toTokenAmount}"
    }
}

data class SwapTransaction(
        val from: Address,
        val to: Address,
        val data: ByteArray,
        val value: BigInteger,
        val gasPrice: Long?,
        val maxFeePerGas: Long?,
        val maxPriorityFeePerGas: Long?,
        @SerializedName("gas") val gasLimit: Long
) {
    override fun toString(): String {
        return "SwapTransaction {\nfrom: ${from.hex}, \nto: ${to.hex}, \ndata: ${data.toHexString()}, \nvalue: $value, \ngasPrice: $gasPrice, \ngasLimit: $gasLimit\n}"
    }
}

data class Swap(
        val fromToken: Token,
        val toToken: Token,
        val fromTokenAmount: BigInteger,
        val toTokenAmount: BigInteger,
        @SerializedName("protocols") val route: List<Any>,
        @SerializedName("tx") val transaction: SwapTransaction
) {
    override fun toString(): String {
        return "Swap {\nfromToken: ${fromToken.name}, \ntoToken: ${toToken.name}, \nfromTokenAmount: $fromTokenAmount, \ntoTokenAmount: $toTokenAmount, \ntx: $transaction\n}"
    }
}

data class ApproveCallData(
        val data: ByteArray,
        val gasPrice: Long,
        val to: Address,
        val value: BigInteger
) {
    override fun equals(other: Any?): Boolean {
        return when {
            this === other -> true
            other is ApproveCallData -> to == other.to && value == other.value && data.contentEquals(other.data)
            else -> false
        }
    }

    override fun hashCode(): Int {
        return Objects.hash(to, value, data)
    }

    override fun toString(): String {
        return "ApproveCallData {\nto: ${to.hex}, \nvalue: $value, \ndata: ${data.toHexString()}\n}"
    }
}

data class Spender(val address: Address)

