/*
 * Copyright (C) 2019 The Android Open Source Project
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
 * limitations under the License.
 */
package com.android.launcher3.uioverrides;

import static com.android.launcher3.LauncherState.NORMAL;
import static com.android.launcher3.LauncherState.OVERVIEW;
import static com.android.quickstep.SysUINavigationMode.Mode.NO_BUTTON;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

import com.android.launcher3.BaseQuickstepLauncher;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.WorkspaceItemInfo;
import com.android.launcher3.anim.AnimatorPlaybackController;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.folder.Folder;
import com.android.launcher3.hybridhotseat.HotseatPredictionController;
import com.android.launcher3.popup.SystemShortcut;
import com.android.launcher3.uioverrides.touchcontrollers.FlingAndHoldTouchController;
import com.android.launcher3.uioverrides.touchcontrollers.LandscapeEdgeSwipeController;
import com.android.launcher3.uioverrides.touchcontrollers.NavBarToHomeTouchController;
import com.android.launcher3.uioverrides.touchcontrollers.NoButtonNavbarToOverviewTouchController;
import com.android.launcher3.uioverrides.touchcontrollers.NoButtonQuickSwitchTouchController;
import com.android.launcher3.uioverrides.touchcontrollers.OverviewToAllAppsTouchController;
import com.android.launcher3.uioverrides.touchcontrollers.PortraitStatesTouchController;
import com.android.launcher3.uioverrides.touchcontrollers.QuickSwitchTouchController;
import com.android.launcher3.uioverrides.touchcontrollers.StatusBarTouchController;
import com.android.launcher3.uioverrides.touchcontrollers.TaskViewTouchController;
import com.android.launcher3.uioverrides.touchcontrollers.TransposedQuickSwitchTouchController;
import com.android.launcher3.util.TouchController;
import com.android.launcher3.util.UiThreadHelper;
import com.android.launcher3.util.UiThreadHelper.AsyncCommand;
import com.android.quickstep.RecentsModel;
import com.android.quickstep.SysUINavigationMode;
import com.android.quickstep.SysUINavigationMode.Mode;
import com.android.quickstep.SystemUiProxy;
import com.android.quickstep.views.RecentsView;

import java.util.ArrayList;
import java.util.stream.Stream;

public class QuickstepLauncher extends BaseQuickstepLauncher {

    public static final boolean GO_LOW_RAM_RECENTS_ENABLED = false;
    /**
     * Reusable command for applying the shelf height on the background thread.
     */
    public static final AsyncCommand SET_SHELF_HEIGHT = (context, arg1, arg2) ->
            SystemUiProxy.INSTANCE.get(context).setShelfHeight(arg1 != 0, arg2);
    private HotseatPredictionController mHotseatPredictionController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (FeatureFlags.ENABLE_HYBRID_HOTSEAT.get()) {
            mHotseatPredictionController = new HotseatPredictionController(this);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        onStateOrResumeChanged();
    }

    @Override
    public boolean startActivitySafely(View v, Intent intent, ItemInfo item,
            @Nullable String sourceContainer) {
        if (mHotseatPredictionController != null) {
            mHotseatPredictionController.setPauseUIUpdate(true);
        }
        return super.startActivitySafely(v, intent, item, sourceContainer);
    }

    @Override
    protected void onActivityFlagsChanged(int changeBits) {
        super.onActivityFlagsChanged(changeBits);

        if ((changeBits & (ACTIVITY_STATE_DEFERRED_RESUMED | ACTIVITY_STATE_STARTED
                | ACTIVITY_STATE_USER_ACTIVE | ACTIVITY_STATE_TRANSITION_ACTIVE)) != 0
                && (getActivityFlags() & ACTIVITY_STATE_TRANSITION_ACTIVE) == 0) {
            onStateOrResumeChanged();
        }

        if (mHotseatPredictionController != null && ((changeBits & ACTIVITY_STATE_STARTED) != 0
                || (changeBits & getActivityFlags() & ACTIVITY_STATE_DEFERRED_RESUMED) != 0)) {
            mHotseatPredictionController.setPauseUIUpdate(false);
        }
    }

    @Override
    public void folderCreatedFromItem(Folder folder, WorkspaceItemInfo itemInfo) {
        super.folderCreatedFromItem(folder, itemInfo);
        if (mHotseatPredictionController != null) {
            mHotseatPredictionController.folderCreatedFromWorkspaceItem(itemInfo, folder.getInfo());
        }
    }

    @Override
    public void folderConvertedToItem(Folder folder, WorkspaceItemInfo itemInfo) {
        super.folderConvertedToItem(folder, itemInfo);
        if (mHotseatPredictionController != null) {
            mHotseatPredictionController.folderConvertedToWorkspaceItem(itemInfo, folder.getInfo());
        }
    }

    @Override
    public Stream<SystemShortcut.Factory> getSupportedShortcuts() {
        if (mHotseatPredictionController != null) {
            return Stream.concat(super.getSupportedShortcuts(),
                    Stream.of(mHotseatPredictionController));
        } else {
            return super.getSupportedShortcuts();
        }
    }

    /**
     * Returns Prediction controller for hybrid hotseat
     */
    public HotseatPredictionController getHotseatPredictionController() {
        return mHotseatPredictionController;
    }

    /**
     * Recents logic that triggers when launcher state changes or launcher activity stops/resumes.
     */
    private void onStateOrResumeChanged() {
        LauncherState state = getStateManager().getState();
        DeviceProfile profile = getDeviceProfile();
        boolean visible = (state == NORMAL || state == OVERVIEW) && isUserActive()
                && !profile.isVerticalBarLayout();
        UiThreadHelper.runAsyncCommand(this, SET_SHELF_HEIGHT, visible ? 1 : 0,
                profile.hotseatBarSizePx);
        if (state == NORMAL) {
            ((RecentsView) getOverviewPanel()).setSwipeDownShouldLaunchApp(false);
        }
    }

    @Override
    public void finishBindingItems(int pageBoundFirst) {
        super.finishBindingItems(pageBoundFirst);
        if (mHotseatPredictionController != null) {
            mHotseatPredictionController.createPredictor();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mHotseatPredictionController != null) {
            mHotseatPredictionController.destroy();
        }
    }

    @Override
    public TouchController[] createTouchControllers() {
        Mode mode = SysUINavigationMode.getMode(this);

        ArrayList<TouchController> list = new ArrayList<>();
        list.add(getDragController());
        if (mode == NO_BUTTON) {
            list.add(new NoButtonQuickSwitchTouchController(this));
            list.add(new NavBarToHomeTouchController(this));
            if (FeatureFlags.ENABLE_OVERVIEW_ACTIONS.get()) {
                list.add(new NoButtonNavbarToOverviewTouchController(this));
            } else {
                list.add(new FlingAndHoldTouchController(this));
            }
        } else {
            if (getDeviceProfile().isVerticalBarLayout()) {
                list.add(new OverviewToAllAppsTouchController(this));
                list.add(new LandscapeEdgeSwipeController(this));
                if (mode.hasGestures) {
                    list.add(new TransposedQuickSwitchTouchController(this));
                }
            } else {
                list.add(new PortraitStatesTouchController(this,
                        mode.hasGestures /* allowDragToOverview */));
                if (mode.hasGestures) {
                    list.add(new QuickSwitchTouchController(this));
                }
            }
        }

        if (!getDeviceProfile().isMultiWindowMode) {
            list.add(new StatusBarTouchController(this));
        }

        list.add(new LauncherTaskViewController(this));
        return list.toArray(new TouchController[list.size()]);
    }

    private static final class LauncherTaskViewController extends
            TaskViewTouchController<Launcher> {

        LauncherTaskViewController(Launcher activity) {
            super(activity);
        }

        @Override
        protected boolean isRecentsInteractive() {
            return mActivity.isInState(OVERVIEW);
        }

        @Override
        protected void onUserControlledAnimationCreated(AnimatorPlaybackController animController) {
            mActivity.getStateManager().setCurrentUserControlledAnimation(animController);
        }
    }
}