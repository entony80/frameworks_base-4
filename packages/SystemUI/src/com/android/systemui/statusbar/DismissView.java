/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.systemui.statusbar;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.Button;
import android.widget.TextView;
import com.android.systemui.R;

public class DismissView extends ExpandableView {

    private Button mClearAllText;
    private boolean mIsVisible;
    private boolean mAnimating;
    private boolean mWillBeGone;

    private final Interpolator mAppearInterpolator = new PathInterpolator(0f, 0.2f, 1f, 1f);
    private final Interpolator mDisappearInterpolator = new PathInterpolator(0f, 0f, 0.8f, 1f);

    public DismissView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mClearAllText = (Button) findViewById(R.id.dismiss_text);
        setInvisible();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        setOutlineProvider(null);
    }

    @Override
    public boolean isTransparent() {
        return true;
    }

    public void performVisibilityAnimation(boolean nowVisible) {
        animateText(nowVisible, null /* onFinishedRunnable */);
    }

    public void performVisibilityAnimation(boolean nowVisible, Runnable onFinishedRunnable) {
        animateText(nowVisible, onFinishedRunnable);
    }

    public boolean isVisible() {
        return mIsVisible || mAnimating;
    }

    /**
     * Animate the text to a new visibility.
     *
     * @param nowVisible should it now be visible
     * @param onFinishedRunnable A runnable which should be run when the animation is
     *        finished.
     */
    private void animateText(boolean nowVisible, final Runnable onFinishedRunnable) {
        if (nowVisible != mIsVisible) {
            // Animate text
            float endValue = nowVisible ? 1.0f : 0.0f;
            Interpolator interpolator;
            if (nowVisible) {
                interpolator = mAppearInterpolator;
            } else {
                interpolator = mDisappearInterpolator;
            }
            mAnimating = true;
            mClearAllText.animate()
                    .alpha(endValue)
                    .setInterpolator(interpolator)
                    .setDuration(260)
                    .withLayer()
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            mAnimating = false;
                            if (onFinishedRunnable != null) {
                                onFinishedRunnable.run();
                            }
                        }
                    });
            mIsVisible = nowVisible;
        } else {
            if (onFinishedRunnable != null) {
                onFinishedRunnable.run();
            }
        }
    }

    public void setInvisible() {
        mClearAllText.setAlpha(0.0f);
        mIsVisible = false;
    }

    @Override
    public void performRemoveAnimation(long duration, float translationDirection,
            Runnable onFinishedRunnable) {
        // TODO: Use duration
        performVisibilityAnimation(false);
    }

    @Override
    public void performAddAnimation(long delay, long duration) {
        // TODO: use delay and duration
        performVisibilityAnimation(true);
    }

    @Override
    public void setScrimAmount(float scrimAmount) {
        // We don't need to scrim the dismissView
    }

    public void setOnButtonClickListener(OnClickListener onClickListener) {
        mClearAllText.setOnClickListener(onClickListener);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    public void cancelAnimation() {
        mClearAllText.animate().cancel();
    }

    public void setOnButtonClickListener(OnClickListener listener) {
        mContent.setOnClickListener(listener);
    }

    public boolean isOnEmptySpace(float touchX, float touchY) {
        return touchX < mContent.getX()
                || touchX > mContent.getX() + mContent.getWidth()
                || touchY < mContent.getY()
                || touchY > mContent.getY() + mContent.getHeight();
    }
}
