/*
 * Copyright (c) 2009-2013, 2015, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *    * Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *    * Neither the name of The Linux Foundation nor
 *      the names of its contributors may be used to endorse or promote
 *      products derived from this software without specific prior written
 *      permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package qcom.fmradio;
import android.util.Log;
import java.io.File;

/** <code>FmTransceiver</code> is the superclass of classes
 * <code>FmReceiver</code> and <code>FmTransmitter</code>
 * @hide
 */
public class FmTransceiver
{
   /* Primary FM States :
    * FM will be in one of the 4 states at any point of time
    *    '0'  - FMState_Turned_Off
    *    '1'  - FMState_Rx_Turned_On
    *    '2'  - FMState_Tx_Turned_On
    *    '3'  - FMState_Srch_InProg
   */
   public static final int FMState_Turned_Off   = 0;
   public static final int FMState_Rx_Turned_On = 1;
   public static final int FMState_Tx_Turned_On = 2;
   public static final int FMState_Srch_InProg  = 3;

   /* Intermediate FM power levels */
   public static final int subPwrLevel_FMRx_Starting = 4;
   public static final int subPwrLevel_FMTx_Starting = 5;
   public static final int subPwrLevel_FMTurning_Off = 6;

   /* Intermediate FM search levels :
    * These are the sub-levels of FM Search operations : seek/scan/auto-preset.
    * Used internally for distinguishing between the various search operations.
   */
   public static final int subSrchLevel_NoSearch       = -1;
   public static final int subSrchLevel_SeekInPrg      = 0;
   public static final int subSrchLevel_ScanInProg     = 1;
   public static final int subSrchLevel_SrchListInProg = 2;
   public static final int subSrchLevel_SrchComplete   = 3;
   public static final int subSrchLevel_SrchAbort      = 4;

   /* Holds the current state of the FM device */
   public static int FMState = FMState_Turned_Off;

   /**
    * FMConfigure FM Radio band setting for US/Europe
    */
   public static final int FM_US_BAND              = 0;
   /**
    * FMConfigure FM Radio band setting for US/Europe
    */
   public static final int FM_EU_BAND              = 1;
   /**
    * FMConfigure FM Radio band setting for Japan
    */
   public static final int FM_JAPAN_STANDARD_BAND  = 2;
   /**
    * FMConfigure FM Radio band setting for Japan-Wideband
    */
   public static final int FM_JAPAN_WIDE_BAND      = 3;
   /**
    * FMConfigure FM Radio band setting for "User defined" band
    */
   public static final int FM_USER_DEFINED_BAND    = 4;

   /**
    * FM channel spacing settings = 200KHz
    */
   public static final int FM_CHSPACE_200_KHZ  =0;
   /**
    * FM channel spacing settings = 100KHz
    */
   public static final int FM_CHSPACE_100_KHZ  =1;
   /**
    * FM channel spacing settings = 50KHz
    */
   public static final int FM_CHSPACE_50_KHZ   =2;

   /**
    * FM de-emphasis/pre-emphasis settings = 75KHz
    */
   public static final int FM_DE_EMP75 = 0;
   /**
    * FM de-emphasis/pre-emphasis settings = 50KHz
    */
   public static final int FM_DE_EMP50 = 1;

   /**
    * RDS standard type: RBDS (North America)
    */
   public static final int FM_RDS_STD_RBDS    =0;
   /**
    * RDS standard type: RDS (Rest of the world)
    */
   public static final int FM_RDS_STD_RDS     =1;
   /**
    * RDS standard type: No RDS
    */
   public static final int FM_RDS_STD_NONE    =2;

   protected static final int FM_RX    =1;
   protected static final int FM_TX    =2;

   private final int READY_EVENT = 0x01;
   private final int TUNE_EVENT = 0x02;
   private final int RDS_EVENT = 0x08;
   private final int MUTE_EVENT = 0x04;
   private final int SEEK_COMPLETE_EVENT = 0x03;

   private static final int V4L2_CID_PRIVATE_BASE = 0x8000000;
   private static final int V4L2_CID_PRIVATE_TAVARUA_ANTENNA   = V4L2_CID_PRIVATE_BASE + 18;
   private static final int V4L2_CID_PRIVATE_TAVARUA_RDSGROUP_MASK = V4L2_CID_PRIVATE_BASE + 6;
   private static final int V4L2_CID_PRIVATE_TAVARUA_SET_NOTCH_FILTER = V4L2_CID_PRIVATE_BASE + 40;

   private final String TAG = "FmTransceiver";
   private final String V4L2_DEVICE = "/dev/radio0";

   protected static int sFd;
   protected FmRxControls mControl;
   protected int mPowerMode;
   protected FmRxEventListner mRxEvents;
   protected FmRxRdsData mRdsData;
   protected FmTxEventListner mTxEvents;

   public static final int ERROR = -1;

   /*==============================================================
   FUNCTION:  acquire
   ==============================================================*/
   /**
   *    Allows access to the V4L2 FM device.
   *
   *    This synchronous call allows a client to use the V4L2 FM
   *    device. This must be the first call issued by the client
   *    before any receiver interfaces can be used.
   *
   *    This call also powers up the FM Module.
   *
   *    @param device String that is path to radio device
   *
   *    @return true if V4L2 FM device acquired, false if V4L2 FM
   *            device could not be acquired, possibly acquired by
   *            other client
   *    @see   #release
   *
   */
   protected boolean acquire(String device){
      boolean bStatus = true;
      if (sFd <= 0) { // if previous open fails fd will be -ve.
         sFd = FmReceiverJNI.acquireFdNative(V4L2_DEVICE);

         if (sFd > 0) {
            Log.d(TAG, "Opened "+ sFd);
            bStatus = true;
         }
         else {
            Log.d(TAG, "Fail to Open "+ sFd);
	    bStatus = false;
         }
      }
      else {
         Log.d(TAG, "Already Opened:" + sFd);
         /*This should be case
          * Where User try to opne the device
          * secondtime.
          * Case where Tx and Rx try to
          * acquire the device
          */
         bStatus = false;
       }
      return (bStatus);
   }

   /*==============================================================
   FUNCTION:  release
   ==============================================================*/
   /**
   *    Releases access to the V4L2 FM device.
   *    <p>
   *    This synchronous call allows a client to release control of
   *    V4L2 FM device.  This function should be called when the FM
   *    device is no longer needed. This should be the last call
   *    issued by the FM client. Once called, the client must call
   *    #acquire to re-aquire the V4L2 device control before the
   *    FM device can be used again.
   *    <p>
   *    Before the client can release control of the FM receiver
   *    interface, it must disable the FM receiver, if the client
   *    enabled it, and unregister any registered callback.  If the
   *    client has ownership of the receiver, it will automatically
   *    be returned to the system.
   *    <p>
   *    This call also powers down the FM Module.
   *    <p>
   *    @param device String that is path to radio device
   *    @return true if V4L2 FM device released, false if V4L2 FM
   *            device could not be released
   *    @see   #acquire
   */
   static boolean release(String device) {
      if (sFd!=0)
      {
         FmReceiverJNI.closeFdNative(sFd);
         sFd = 0;
         Log.d("FmTransceiver", "Turned off: " + sFd);
      } else
      {
         Log.d("FmTransceiver", "Error turning off");
      }
      return true;
   }

   /*==============================================================
   FUNCTION:  registerClient
   ==============================================================*/
   /**
   *    Registers a callback for FM receiver event notifications.
   *    <p>
   *    This is a synchronous call used to register for event
   *    notifications from the FM receiver driver. Since the FM
   *    driver performs some tasks asynchronously, this function
   *    allows the client to receive information asynchronously.
   *    <p>
   *    When calling this function, the client must pass a callback
   *    function which will be used to deliver asynchronous events.
   *    The argument callback must be a non-NULL value.  If a NULL
   *    value is passed to this function, the registration will
   *    fail.
   *    <p>
   *    The client can choose which events will be sent from the
   *    receiver driver by simply implementing functions for events
   *    it wishes to receive.
   *    <p>
   *
   *    @param callback the callback to handle the events events
   *                    from the FM receiver.
   *    @return true if Callback registered, false if Callback
   *            registration failed.
   *
   *    @see #acquire
   *    @see #unregisterClient
   *
   */
   public boolean registerClient(FmRxEvCallbacks callback){
      boolean bReturnStatus = false;
      if (callback!=null)
      {
         mRxEvents.startListner(sFd, callback);
         bReturnStatus = true;
      } else
      {
         Log.d(TAG, "Null, do nothing");
      }
      return bReturnStatus;
   }

   /*==============================================================
   FUNCTION:  unregisterClient
   ==============================================================*/
   /**
   *    Unregisters a client's event notification callback.
   *    <p>
   *    This is a synchronous call used to unregister a client's
   *    event callback.
   *    <p>
   *    @return true always.
   *
   *    @see  #acquire
   *    @see  #release
   *    @see  #registerClient
   *
   */
   public boolean unregisterClient () {
      mRxEvents.stopListener();
      return true;
   }


   /*==============================================================
   FUNCTION:  registerTransmitClient
   ==============================================================*/
   /**
   *    Registers a callback for FM Transmitter event
   *    notifications.
   *    <p>
   *    This is a synchronous call used to register for event
   *    notifications from the FM Transmitter driver. Since the FM
   *    driver performs some tasks asynchronously, this function
   *    allows the client to receive information asynchronously.
   *    <p>
   *    When calling this function, the client must pass a callback
   *    function which will be used to deliver asynchronous events.
   *    The argument callback must be a non-NULL value.  If a NULL
   *    value is passed to this function, the registration will
   *    fail.
   *    <p>
   *    The client can choose which events will be sent from the
   *    receiver driver by simply implementing functions for events
   *    it wishes to receive.
   *    <p>
   *
   *    @param callback the callback to handle the events events
   *                    from the FM Transmitter.
   *    @return true if Callback registered, false if Callback
   *            registration failed.
   *
   *    @see #acquire
   *    @see #unregisterTransmitClient
   *
   */
   public boolean registerTransmitClient( FmTransmitterCallbacks callback){
      boolean bReturnStatus = false;
      if (callback!=null)
      {
         mTxEvents.startListner(sFd, callback);
         bReturnStatus = true;
      } else
      {
         Log.d(TAG, "Null, do nothing");
      }
      return bReturnStatus;
   }

   /*==============================================================
   FUNCTION:  unregisterTransmitClient
   ==============================================================*/
   /**
   *    Unregisters Transmitter event notification callback.
   *    <p>
   *    This is a synchronous call used to unregister a Transmitter
   *    client's event callback.
   *    <p>
   *    @return true always.
   *
   *    @see  #acquire
   *    @see  #release
   *    @see  #registerTransmitClient
   *
   */
   public boolean unregisterTransmitClient () {
      mTxEvents.stopListener();
      return true;
   }

   /*==============================================================
   FUNCTION:  enable
   ==============================================================*/
   /**
   *    Initializes the FM device.
   *    <p>
   *    This is a synchronous call is used to initialize the FM
   *    tranceiver. If already initialized this function will
   *    intialize the tranceiver with default settings. Only after
   *    successfully calling this function can many of the FM device
   *    interfaces be used.
   *    <p>
   *    When enabling the receiver, the client must also provide
   *    the regional settings in which the receiver will operate.
   *    These settings (included in configSettings) are typically
   *    used for setting up the FM receiver for operating in a
   *    particular geographical region. These settings can be
   *    changed after the FM driver is enabled through the use of
   *    the function #configure.
   *    <p>
   *    This call can only be issued by the owner of an FM
   *    receiver.  To issue this call, the client must first
   *    successfully call #acquire.
   *    <p>
   *    @param configSettings  the settings to be applied when
   *                             turning on the radio
   *    @return true if Initialization succeeded, false if
   *            Initialization failed.
   *    @see   #registerClient
   *    @see   #disable
   *
   */
   public boolean enable (FmConfig configSettings, int device){

      boolean status;
      int ret;

      if (!FmReceiver.isCherokeeChip()) {
          //Acquire the deviceon Enable
          if (!acquire("/dev/radio0")) {
              return false;
          }
      }
      if (new File("/etc/fm/SpurTableFile.txt").isFile()) {
          Log.d(TAG, "Send Spur roation table");
          FmConfig.fmSpurConfig(sFd);
      } else {
          Log.d(TAG, "No existing file to do spur configuration");
      }
      Log.d(TAG, "turning on " + device);
      ret = mControl.fmOn(sFd, device);
      if (ret < 0) {
          Log.d(TAG, "turning on failed");
          FmReceiverJNI.closeFdNative(sFd);
          sFd = 0;
          return false;
      }

      Log.d(TAG, "Calling fmConfigure");
      status = FmConfig.fmConfigure (sFd, configSettings);
      if (!status) {
          Log.d(TAG, "fmConfigure failed");
          FmReceiverJNI.closeFdNative(sFd);
          sFd = 0;
      }
      return status;
   }

   /*==============================================================
   FUNCTION:  disable
   ==============================================================*/
   /**
   *    Disables the FM Device.
   *    <p>
   *    This is a synchronous call used to disable the FM
   *    device. This function is expected to be used when the
   *    client no longer requires use of the FM device. Once
   *    called, most functionality offered by the FM device will be
   *    disabled until the client re-enables the device again via
   *    #enable.
   *    <p>
   *    @return true if disabling succeeded, false if disabling
   *            failed.
   *    <p>
   *    @see   #enable
   *    @see   #registerClient
   */
   public boolean disable(){
      mControl.fmOff(sFd);
      return true;
   }

   /*==============================================================
   FUNCTION:  configure
   ==============================================================*/
   /**
   *     Reconfigures the device's regional settings
   *    (FM Band, De-Emphasis, Channel Spacing, RDS/RBDS mode).
   *    <p>
   *    This is a synchronous call used to reconfigure settings on
   *    the FM device. Included in the passed structure are
   *    settings which typically differ from one geographical
   *    region to another.
   *    <p>
   *    @param configSettings    Contains settings for the FM radio
   *                             (FM band, De-emphasis, channel
   *                             spacing, RDS/RBDS mode)
   *    <p>
   *    @return      true if configure succeeded, false if
   *                 configure failed.
   */
   public boolean configure(FmConfig configSettings){
      boolean status=true;
      int lowerFreq = configSettings.getLowerLimit();
      Log.d(TAG, "fmConfigure");
      status = FmConfig.fmConfigure (sFd, configSettings);
      status = setStation (lowerFreq);
      return status;
   }

   /*==============================================================
   FUNCTION:  setStation
   ==============================================================*/
   /**
    *    Tunes the FM device to the specified FM frequency.
    *    <p>
    *    This method tunes the FM device to a station specified by the
    *    provided frequency. Only valid frequencies within the band
    *    set by enable or configure can be tuned by this function.
    *    Attempting to tune to frequencies outside of the set band
    *    will result in an error.
    *    <p>
    *    Once tuning to the specified frequency is completed, the
    *    event callback FmRxEvRadioTuneStatus will be called.
    *
    *    @param frequencyKHz  Frequency (in kHz) to be tuned
    *                         (Example: 96500 = 96.5Mhz)
    *   @return true if setStation call was placed successfully,
    *           false if setStation failed.
    */
   public boolean setStation (int frequencyKHz) {
      int ret;

      mControl.setFreq(frequencyKHz);
      ret = mControl.setStation(sFd);
      if(ret < 0 )
      {
         return false;
      }
      else
      {
         return true;
      }
   }

   /*==============================================================
   FUNCTION:  SetNotchFilter
   ==============================================================*/
   /**
    *    Sets the desired notch filter for WAN avoidance.
    *    <p>
    *    This method sets the required Notch filter based on the current
    *    WAN band frequency to achieve the FM-WAN concurrency.
    *    Application should listen to Data call events and call the function
    *    on every data call connection set-u, to achieve the FM-WAN concurrency.
    *
    */
   public void setNotchFilter(boolean value) {
	FmReceiverJNI.setNotchFilterNative(sFd, V4L2_CID_PRIVATE_TAVARUA_SET_NOTCH_FILTER, value);
   }

   /*==============================================================
   FUNCTION:  SetAnalogMode
   ==============================================================*/
   /**
    *    Enable/Disable the Analog lowpower mode.
    *    <p>
    *    This method enables/disables the analog lowpower mode.
    *
    */
   public boolean setAnalogMode(boolean value) {
        int re = mControl.setAudioPath(sFd, value);
        re = FmReceiverJNI.setAnalogModeNative(value);
        if (re == 1)
            return true;
        return false;
   }

   /*==============================================================
   FUNCTION:  getInternalAntenna
   ==============================================================*/
   /**
   *    Returns true if internal FM antenna is available
   *
   *    <p>
   *    This method returns true is internal FM antenna is
   *    available, false otherwise
   *
   *    <p>
   *    @return    true/false
   */
   public boolean getInternalAntenna()
   {

       int re = FmReceiverJNI.getControlNative (sFd, V4L2_CID_PRIVATE_TAVARUA_ANTENNA);

       if (re == 1)
         return true;

       return false;
   }

   /*==============================================================
   FUNCTION:  setInternalAntenna
   ==============================================================*/
   /**
   *    Returns true if successful, false otherwise
   *
   *    <p>
   *    This method sets internal antenna type to true/false
   *
   *    @param intAntenna true is Internal antenna is present
   *
   *    <p>
   *    @return    true/false
   */
   public boolean setInternalAntenna(boolean intAnt)
   {

       int iAntenna ;

       if (intAnt)
          iAntenna = 1;
       else
          iAntenna = 0;


       int re = FmReceiverJNI.setControlNative (sFd, V4L2_CID_PRIVATE_TAVARUA_ANTENNA, iAntenna);

       if (re == 0)
         return true;

       return false;
   }
/*==============================================================
   FUNCTION:  setFMPowerState
   ==============================================================*/
   /**
   *    Sets the FM power state
   *
   *    <p>
   *    This method sets the FM power state.
   *
   *    <p>
   */
   static void setFMPowerState(int state)
   {
      FMState = state;
   }
/*==============================================================
   FUNCTION:  getFMPowerState
   ==============================================================*/
   /**
   *    Returns :
   *
   *        FMOff        - If the FM Radio is turned off
   *        FMRxOn       - If the FM Receiver is currently turned on
   *        FMTxOn       - If the FM Transmitter is currently turned on
   *        FMReset      - If the FM Radio is reset
   *
   *    Gets the FM power state
   *
   *    <p>
   *    This method gets the FM power state.
   *
   *    <p>
   */
   public static int getFMPowerState()
   {
      return FMState;
   }
   public static boolean setRDSGrpMask(int mask)
   {
      int re;
      re = FmReceiverJNI.setControlNative(sFd,
                   V4L2_CID_PRIVATE_TAVARUA_RDSGROUP_MASK, mask);
      if (re == 0)
          return true;
      else
          return false;
   }
}
