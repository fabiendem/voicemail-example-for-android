/*
 * Copyright (C) 2011 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package com.google.android.voicemail.example.dependency;

import com.google.android.voicemail.example.fake.FakeVoicemailFetcher;
import com.google.android.voicemail.example.imap.AsyncImapVoicemailFetcher;
import com.google.android.voicemail.example.proxy.SmsManagerProxyImpl;
import com.google.android.voicemail.example.proxy.TelephonyManagerProxy;
import com.google.android.voicemail.example.proxy.TelephonyManagerProxyImpl;
import com.google.android.voicemail.example.sms.OmtpMessageHandler;
import com.google.android.voicemail.example.sms.OmtpMessageHandlerImpl;
import com.google.android.voicemail.example.sms.OmtpMessageSender;
import com.google.android.voicemail.example.sms.OmtpMessageSenderImpl;
import com.google.android.voicemail.example.sms.SmsParser;
import com.google.android.voicemail.example.sms.SmsParserImpl;
import com.google.android.voicemail.example.spec.ProviderConfig;
import com.google.android.voicemail.example.spec.ProviderConfigs;
import com.google.android.voicemail.example.storage.AccountsDatabase;
import com.google.android.voicemail.example.sync.LocalVvmStore;
import com.google.android.voicemail.example.sync.OmtpVvmStore;
import com.google.android.voicemail.example.sync.SyncResolver;
import com.google.android.voicemail.example.sync.SyncResolverImpl;
import com.google.android.voicemail.example.sync.VoicemailFetcherFactory;
import com.google.android.voicemail.example.sync.VvmStore;
import com.google.android.voicemail.example.sync.VvmStoreResolverImpl;
import com.google.android.voicemail.example.util.AccountStoreWrapper;
import com.google.android.voicemail.example.util.AccountStoreWrapperImpl;
import com.google.android.voicemail.example.util.UserSettings;
import com.google.android.voicemail.example.util.VoicemailFetcher;

import android.content.Context;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;

import com.example.android.voicemail.common.core.VoicemailProviderHelper;
import com.example.android.voicemail.common.core.VoicemailProviderHelpers;
import com.example.android.voicemail.common.logging.Logger;

import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Concrete implementation of {@link DependencyResolver}.
 * <p>
 * There is only a single instance of this class for the whole application.
 */
@ThreadSafe
public final class DependencyResolverImpl implements DependencyResolver {
    private static final Logger logger = Logger.getLogger(DependencyResolver.class);

    private static DependencyResolverImpl sSingletonInstance;

    /**
     * Initialize the dependency resolver with the application's context. This context is typically
     * used to instantiate singleton instances of objects that needs to be created using a global
     * application context.
     *
     * @param applicationContext The global application context
     * @throws IllegalStateException If the class is already initialized
     */
    public static synchronized void initialize(Context applicationContext) {
        if (sSingletonInstance == null) {
            sSingletonInstance = new DependencyResolverImpl(applicationContext);
            logger.i("Dependency resolver created for " + applicationContext.getPackageName());
        } else {
            throw new IllegalStateException("DependencyResolverImpl already initialized");
        }
    }

    /**
     * Returns the only instance of {@link DependencyResolverImpl}.
     * <p>
     * Make sure that the class has already been initialized by calling
     * {@link DependencyResolverImpl#initialize(Context)} before getting its instance.
     *
     * @throws IllegalStateException If {@link DependencyResolverImpl#initialize(Context)} is not
     *             yet called
     */
    public static synchronized DependencyResolverImpl getInstance() {
        if (sSingletonInstance == null) {
            throw new IllegalStateException("DependencyResolverImpl not yet intialized."
                    + "Call DependencyResolverImpl.initialize()");
        }
        return sSingletonInstance;
    }

    private final Context mAppContext;
    private ExecutorService mExecutorService;
    private AccountStoreWrapper mAccountStore;
    private VoicemailFetcherFactory mVoicemailFetcherFactory;
    private UserSettings mUserSettings;
    private VvmStore mLocalStore;
    private VvmStore mRemoteStore;

    private DependencyResolverImpl(Context appContext) {
        mAppContext = appContext;
    }

    @Override
    public Context getApplicationContext() {
        return mAppContext;
    }

    @Override
    public synchronized ExecutorService getExecutorService() {
        if (mExecutorService == null) {
            mExecutorService = Executors.newCachedThreadPool();
        }
        return mExecutorService;
    }

    @Override
    public synchronized AccountStoreWrapper getAccountsStore() {
        if (mAccountStore == null) {
            mAccountStore = new AccountStoreWrapperImpl(new AccountsDatabase(mAppContext),
                    createTelephonyManagerProxy());
        }
        return mAccountStore;
    }

    @Override
    public synchronized VoicemailFetcherFactory getVoicemailFetcherFactory() {
        if (mVoicemailFetcherFactory == null) {
            mVoicemailFetcherFactory = createVoicemailFetcherFactory();
        }
        return mVoicemailFetcherFactory;
    }

    @Override
    public synchronized UserSettings getUserSettings() {
        if (mUserSettings == null) {
            mUserSettings = new UserSettings(
                    PreferenceManager.getDefaultSharedPreferences(mAppContext), mAppContext);
        }
        return mUserSettings;
    }

    @Override
    public synchronized VvmStore getLocalStore() {
        if (mLocalStore == null) {
            mLocalStore = createLocalStore();
        }
        return mLocalStore;
    }

    @Override
    public synchronized VvmStore getRemoteStore() {
        if (mRemoteStore == null) {
            mRemoteStore = createRemoteStore();
        }
        return mRemoteStore;
    }

    private TelephonyManagerProxy createTelephonyManagerProxy() {
        return new TelephonyManagerProxyImpl(
                (TelephonyManager) mAppContext.getSystemService(Context.TELEPHONY_SERVICE));
    }

    /**
     * To be used only in test code to inject a mock/fake instance of {@link AccountStoreWrapper}.
     */
    public synchronized void setAccountStoreForTest(AccountStoreWrapper accountStore) {
        if (mAccountStore != null) {
            throw new IllegalStateException(
                    "accountStore already set. Can no more change its value.");
        }
        mAccountStore = accountStore;
    }

    /**
     * Determines the name of the provider that should be used with the currently inserted SIM.
     * <p>
     * This is typically the OMTP provider that corresponds to the carrier.
     */
    // TODO: Currently this returns a hard-coded value. Eventually we plan to implement a dynamic
    // logic, which might be based on carrier's details from the SIM.
    private String getCarrierProviderName() {
        return ProviderConfigs.MY_CARRIER;
    }

    @Override
    public OmtpMessageSender createOmtpMessageSender() {
        ProviderConfig config = ProviderConfigs.getProviderConfig(getCarrierProviderName());
        return new OmtpMessageSenderImpl(new SmsManagerProxyImpl(SmsManager.getDefault()),
                getAccountsStore(), config);
    }

    @Override
    public OmtpMessageHandler createOmtpMessageHandler() {
        return new OmtpMessageHandlerImpl(getLocalStore(), getAccountsStore(), createSyncResolver(),
                createSmsParser());
    }

    @Override
    public SmsParser createSmsParser() {
        ProviderConfig config = ProviderConfigs.getProviderConfig(getCarrierProviderName());
        return new SmsParserImpl(new SimpleDateFormat(config.getDateFormat()));
    }

    @Override
    public SyncResolver createSyncResolver() {
        return new SyncResolverImpl(new VvmStoreResolverImpl(),
                new VvmStoreResolverImpl.DefaultResolvePolicy(),
                getRemoteStore(),
                getLocalStore());
    }

    private VoicemailFetcherFactory createVoicemailFetcherFactory() {
        return new VoicemailFetcherFactory() {
            @Override
            public VoicemailFetcher createVoicemailFetcher() {
                if (getUserSettings().isFakeModeEnabled()) {
                    return new FakeVoicemailFetcher(getExecutorService(), createSmsParser());
                } else {
                    return new AsyncImapVoicemailFetcher(mAppContext, getExecutorService(),
                            getAccountsStore());
                }
            }
        };
    }

    private VvmStore createRemoteStore() {
        return new OmtpVvmStore(getVoicemailFetcherFactory(), getExecutorService(), mAppContext);
    }

    private VvmStore createLocalStore() {
        VoicemailProviderHelper providerHelper =
                VoicemailProviderHelpers.createPackageScopedVoicemailProvider(mAppContext);
        return new LocalVvmStore(getExecutorService(), providerHelper, mAppContext);
    }

}
