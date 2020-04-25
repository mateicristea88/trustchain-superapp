package nl.tudelft.trustchain.currencyii.ui.bitcoin

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_bitcoin.*
import nl.tudelft.trustchain.currencyii.R
import nl.tudelft.trustchain.currencyii.coin.*
import nl.tudelft.trustchain.currencyii.ui.BaseFragment
import org.bitcoinj.core.NetworkParameters
import java.io.File

/**
 * A simple [Fragment] subclass.
 * Use the [BitcoinFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@Suppress("DEPRECATION")
class BitcoinFragment : BaseFragment(R.layout.fragment_bitcoin),
    ImportKeyDialog.ImportKeyDialogListener {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        handleWalletNavigation()
        val navController = findNavController()
        val args = BitcoinFragmentArgs.fromBundle(requireArguments())
        if (args.showDownload) {
            navController.navigate(BitcoinFragmentDirections.actionBitcoinFragmentToBlockchainDownloadFragment())
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initClickListeners()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()
        Log.i("Coin", "Resuming")
        refresh()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        // TODO: Try catch not too nice.
        try {
            WalletManagerAndroid.getInstance()
        } catch (e: IllegalStateException) {
            Log.w("Coin", "Wallet Manager not initialized.")
            return
        }

        inflater.inflate(R.menu.bitcoin_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.item_bitcoin_blockchain_download -> {
                Log.i("Coin", "Navigating from BitcoinFragment to BlockchainDownloadFragment")
                findNavController().navigate(BitcoinFragmentDirections.actionBitcoinFragmentToBlockchainDownloadFragment())
                true
            }
            R.id.item_dao_overview_refresh -> {
                this.refresh(true)
                Log.i(
                    "Coin",
                    WalletManagerAndroid.getInstance().kit.wallet().toString(
                        true,
                        false,
                        false,
                        null
                    )
                )
                true
            }
            R.id.item_dao_logout -> {
                Log.i("Coin", "Logging out of current DAO user")
                findNavController().navigate(BitcoinFragmentDirections.actionBitcoinFragmentToDaoLoginChoice())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun handleWalletNavigation() {
        val navController = findNavController()

        val vWalletFileMainNet = File(
            this.requireContext().applicationContext.filesDir,
            "$MAIN_NET_WALLET_NAME.wallet"
        )

        val vWalletFileTestNet = File(
            this.requireContext().applicationContext.filesDir,
            "$TEST_NET_WALLET_NAME.wallet"
        )

        if ((vWalletFileMainNet.exists() && vWalletFileMainNet.exists()) && !WalletManagerAndroid.isInitialized()) {
            navController.navigate(R.id.daoLoginChoice)
        } else if ((vWalletFileMainNet.exists() || vWalletFileTestNet.exists()) && !WalletManagerAndroid.isInitialized()) {
            val params = when (vWalletFileTestNet.exists()) {
                true -> BitcoinNetworkOptions.TEST_NET
                false -> BitcoinNetworkOptions.PRODUCTION
            }
            val config = WalletManagerConfiguration(params)
            WalletManagerAndroid.Factory(this.requireContext().applicationContext)
                .setConfiguration(config).init()
            navController.navigate(R.id.blockchainDownloadFragment)
        } else if (!vWalletFileMainNet.exists() && !vWalletFileTestNet.exists()) {
            navController.navigate(R.id.daoLoginChoice)
        }
    }

    private fun initClickListeners() {
        button_copy_public_address.setOnClickListener {
            val walletManager = WalletManagerAndroid.getInstance()
            copyToClipboard(walletManager.protocolAddress().toString())
        }

        button_copy_wallet_seed.setOnClickListener {
            val walletManager = WalletManagerAndroid.getInstance()
            val seed = walletManager.toSeed()
            copyToClipboard("${seed.seed}, ${seed.creationTime}")
        }

        bitcoin_refresh_swiper.setOnRefreshListener {
            this.refresh()
            Handler().postDelayed({
                try {
                    bitcoin_refresh_swiper.isRefreshing = false
                } catch (e: IllegalStateException) {
                }
            }, 1500)
        }
    }

    private fun refresh(animation: Boolean? = false) {
        if (animation!!) {
            bitcoin_refresh_swiper.isRefreshing = true
            Handler().postDelayed({
                try {
                    bitcoin_refresh_swiper.isRefreshing = false
                } catch (e: IllegalStateException) {
                }
            }, 1500)
        }

        if (!WalletManagerAndroid.isRunning) {
            return
        }

        var walletManager = WalletManagerAndroid.getInstance()

        walletBalance.text =
            "${walletManager.kit.wallet().balance.toFriendlyString()}"
        if (walletManager.params.id === NetworkParameters.ID_MAINNET) {
            chosenNetwork.text = "Production Network"
        } else {
            chosenNetwork.text = "Test Network"
        }
        val seed = walletManager.toSeed()
        walletSeed.text = "${seed.seed}, ${seed.creationTime}"
        yourPublicHex.text = walletManager.networkPublicECKeyHex()
        protocolKey.text = walletManager.protocolAddress().toString()

        requireActivity().invalidateOptionsMenu()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        showNavBar()
        return inflater.inflate(R.layout.fragment_bitcoin, container, false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment bitcoinFragment.
         */
        @JvmStatic
        fun newInstance() = BitcoinFragment()
    }

    override fun onImport(address: String, privateKey: String, testNet: Boolean) {
        if (!WalletManagerAndroid.isRunning) {
            val config = WalletManagerConfiguration(
                if (testNet) BitcoinNetworkOptions.TEST_NET else BitcoinNetworkOptions.PRODUCTION,
                null,
                AddressPrivateKeyPair(address, privateKey)
            )

            try {
                WalletManagerAndroid.Factory(this.requireContext().applicationContext)
                    .setConfiguration(config)
                    .init()
            } catch (t: Throwable) {
                Toast.makeText(
                    this.requireContext(),
                    "Something went wrong while initializing the new wallet. ${t.message
                        ?: "No further information"}.",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
        } else {
            WalletManagerAndroid.getInstance().addKey(privateKey)
        }
    }

    override fun onImportDone() {
        this.refresh(true)
        Handler().postDelayed({
            findNavController().navigate(BitcoinFragmentDirections.actionBitcoinFragmentToBlockchainDownloadFragment())
        }, 1500)
    }

    fun copyToClipboard(text: String) {
        val clipboard = getSystemService(this.requireContext(), ClipboardManager::class.java)!!
        val clip = ClipData.newPlainText(text, text)
        clipboard.setPrimaryClip(clip)
    }
}
