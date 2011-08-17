package com.github.fabischmid.horizontalpager.withpagecontrol;


import android.app.Activity;
import android.os.Bundle;

public class HorizontalPagerWithPageControlActivity extends Activity {
	private HorizontalPagerWithPageControl mPager;
//    private RadioGroup mRadioGroup;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.horizontal_pager_with_page_control);
        mPager = (HorizontalPagerWithPageControl) findViewById(R.id.horizontal_pager);
        /*
         * You can add more views to the horizontal pager here with mPager.addChild() or in xml.
         * When every view is in the horizontal pager just call addPagerControl() on the
         * horzizontal pager. Thats all :)
         */
        mPager.addPagerControl();
        
//        mPager.setOnScreenSwitchListener(onScreenSwitchListener);
//        mRadioGroup = (RadioGroup) findViewById(R.id.tabs);
//        mRadioGroup.setOnCheckedChangeListener(onCheckedChangedListener);
    }
/*
 * /////////////////////////////////
 * RADIOGROUPS ARE ONLY UNCOMMENTED! 
 * look at xml 
 * /////////////////////////////////
 */
    
//    private final HorizontalPagerWithPageControl.OnScreenSwitchListener onScreenSwitchListener =
//            new HorizontalPagerWithPageControl.OnScreenSwitchListener() {
//                @Override
//                public void onScreenSwitched(final int screen) {
//                    // Check the appropriate button when the user swipes screens.
//                    switch (screen) {
//                        case 0:
//                            mRadioGroup.check(R.id.radio_btn_0);
//                            break;
//                        case 1:
//                            mRadioGroup.check(R.id.radio_btn_1);
//                            break;
//                        case 2:
//                            mRadioGroup.check(R.id.radio_btn_2);
//                            break;
//                        default:
//                            break;
//                    }
//                }
//            };
//
//    private final RadioGroup.OnCheckedChangeListener onCheckedChangedListener =
//            new RadioGroup.OnCheckedChangeListener() {
//                @Override
//                public void onCheckedChanged(final RadioGroup group, final int checkedId) {
//                    // Slide to the appropriate screen when the user checks a button.
//                    switch (checkedId) {
//                        case R.id.radio_btn_0:
//                            mPager.setCurrentScreen(0, true);
//                            break;
//                        case R.id.radio_btn_1:
//                            mPager.setCurrentScreen(1, true);
//                            break;
//                        case R.id.radio_btn_2:
//                            mPager.setCurrentScreen(2, true);
//                            break;
//                        default:
//                            break;
//                    }
//                }
//            };
}