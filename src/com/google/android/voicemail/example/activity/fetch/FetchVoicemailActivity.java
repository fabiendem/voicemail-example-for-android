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
package com.google.android.voicemail.example.activity.fetch;

import com.google.android.voicemail.example.R;
import com.google.android.voicemail.example.activity.SettingsActivity;
import com.google.android.voicemail.example.dependency.DependencyResolver;
import com.google.android.voicemail.example.dependency.DependencyResolverImpl;
import com.google.android.voicemail.example.sms.OmtpMessageSender;
import com.google.android.voicemail.example.util.AccountStoreWrapper;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.provider.CallLog;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.android.voicemail.common.core.VoicemailProviderHelper;
import com.example.android.voicemail.common.core.VoicemailProviderHelpers;
import com.example.android.voicemail.common.inject.InjectView;
import com.example.android.voicemail.common.inject.Injector;
import com.example.android.voicemail.common.ui.DialogHelperImpl;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * Simple test activity for fetching audio attachments via IMAP.
 * <p>
 * This class is not thread safe. It handles concurrency via thread-confinement, methods on this
 * class should only ever be called from the ui thread.
 */
@NotThreadSafe
public class FetchVoicemailActivity extends Activity
        implements FetchVoicemailPresenter.VVMImapView {
    // IMAP related UI objects.
    @InjectView(R.id.upload_greeting_button) private Button mUploadButton;
    @InjectView(R.id.sync_messages_button) private Button mSyncMessagesButton;
    @InjectView(R.id.sync_progress_bar) private ProgressBar mSyncProgressBar;

    // SMS related UI objects.
    @InjectView(R.id.activate_vvm_button) private Button mActivateVvmButton;
    @InjectView(R.id.deactivate_vvm_button) private Button mDeactivateVvmButton;
    @InjectView(R.id.get_vvm_status_button) private Button mGetVvmStatusButton;
    @InjectView(R.id.sms_progress_bar) private ProgressBar mSmsProgressBar;
    @InjectView(R.id.sms_progress_text) private TextView mSmsProgressText;

    private final DialogHelperImpl mDialogHelper = new DialogHelperImpl(this);
    private FetchVoicemailPresenter mPresenter;
    private VoicemailProviderHelper mVoicemailProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        Injector.get(this).inject();

        mVoicemailProvider = VoicemailProviderHelpers.createPackageScopedVoicemailProvider(this);

        DependencyResolver dependencyResolver = DependencyResolverImpl.getInstance();
        AccountStoreWrapper accountStore = dependencyResolver.getAccountsStore();
        OmtpMessageSender messageSender = dependencyResolver.createOmtpMessageSender();
        mPresenter = new FetchVoicemailPresenter(this, this, mDialogHelper, mVoicemailProvider,
                dependencyResolver.createSyncResolver(), accountStore, messageSender);
        mPresenter.onCreate(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.menu_clear_items:
                mPresenter.menuClearItemsSelected();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        return mDialogHelper.handleOnCreateDialog(id, args);
    }

    @Override
    public void setUploadButtonListener(View.OnClickListener listener) {
        mUploadButton.setOnClickListener(listener);
    }

    @Override
    public void setSyncMessagesButtonListener(OnClickListener listener) {
        mSyncMessagesButton.setOnClickListener(listener);
    }

    @Override
    public void setActivateVvmButtonListener(OnClickListener listener) {
        mActivateVvmButton.setOnClickListener(listener);
    }

    @Override
    public void setDeactivateVvmButtonListener(OnClickListener listener) {
        mDeactivateVvmButton.setOnClickListener(listener);
    }

    @Override
    public void setGetVvmStatusButtonListener(OnClickListener listener) {
        mGetVvmStatusButton.setOnClickListener(listener);
    }

    @Override
    public void setSyncCompleted() {
        mSyncMessagesButton.setText(R.string.sync_completed);
        mSyncMessagesButton.setEnabled(true);
        mSyncProgressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void setSyncInProgress() {
        mSyncMessagesButton.setText(R.string.sync_in_progress);
        mSyncMessagesButton.setEnabled(false);
        mSyncProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void setSmsSendCompleted() {
        mSmsProgressText.setVisibility(View.INVISIBLE);
        mSmsProgressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void setSmsSendInProgress() {
        mSmsProgressText.setVisibility(View.VISIBLE);
        mSmsProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void goToVoicemailListAndFinish() {
        Intent intent = new Intent(Intent.ACTION_VIEW, CallLog.Calls.CONTENT_URI);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
