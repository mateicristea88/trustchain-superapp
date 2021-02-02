package com.example.musicdao.wallet

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.musicdao.MusicService
import com.example.musicdao.R
import kotlinx.android.synthetic.main.fragment_wallet.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * This shows details of the user's wallet
 */
class WalletFragment : Fragment(R.layout.fragment_wallet) {
    private lateinit var walletService: WalletService
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setMenuVisibility(false)
        val walletDir = context?.cacheDir ?: throw Error("CacheDir not found")
        walletService = WalletService.getInstance(walletDir, (activity as MusicService))

        lifecycleScope.launchWhenStarted {
            while (isActive) {
                wallet_public_key.text = walletService.publicKeyText()
                wallet_balance.text = walletService.balanceText()

                val progress = walletService.percentageSynced
                if (progress < 100) {
                    blockchain_progress.progress = progress
                    wallet_status.text = "Syncing chain... progress: $progress%"
                }

                if (progress == 100) {
                    blockchain_progress.progress = 100
                    wallet_status.text = "Finished syncing chain"
                }

                wallet_status_2.text = walletService.status()
                delay(1000)
            }
        }
    }
}
