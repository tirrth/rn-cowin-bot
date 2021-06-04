/**
 * @format
 */

import {
  AppRegistry,
  Button,
  View,
  NativeModules,
  PermissionsAndroid,
} from 'react-native';
import DeviceInfo from 'react-native-device-info';
import {name as appName} from './app.json';
import React, {useEffect, useState} from 'react';

const {ServiceModule} = NativeModules;

const requestMessagingPermission = async () => {
  try {
    const granted = await PermissionsAndroid.request(
      PermissionsAndroid.PERMISSIONS.RECEIVE_SMS,
    );
    if (granted === PermissionsAndroid.RESULTS.GRANTED) {
      console.log('You can use the messaging service');
    } else {
      console.log('Messaging permission denied');
    }
  } catch (err) {
    console.warn(err);
  }
};

const requestPhoneStatePermission = async () => {
  try {
    const granted = await PermissionsAndroid.request(
      PermissionsAndroid.PERMISSIONS.READ_PHONE_STATE,
    );
    if (granted === PermissionsAndroid.RESULTS.GRANTED) {
      console.log('You can read phone-state');
    } else {
      console.log('Reading phone-state permission denied');
    }
  } catch (err) {
    console.warn(err);
  }
};

const ListenMessageApp = () => {
  const {
    startService,
    stopService,
    isServiceRunning: currentServiceState,
  } = ServiceModule;
  const [isServiceRunning, setServiceRunner] = useState(false);

  useEffect(() => {
    (async () => {
      await requestPhoneStatePermission();
      await requestMessagingPermission();
      DeviceInfo.getPhoneNumber()
        .then(res => console.log(`${res}`))
        .catch(err => console.log(err));
      const isServiceRunning = currentServiceState();
      setServiceRunner(isServiceRunning);
    })();
  }, []);

  const _startService = () => {
    startService({
      mobile: '9106132870',
      refresh_interval: '3',
      pincodes: ['382350', '382345', '380050', '380045', '382323'],
      district_ids: ['154', '174', '158', '175', '181'],
    });
    setServiceRunner(true);
  };

  const _stopService = () => {
    stopService();
    setServiceRunner(false);
  };

  return (
    <View>
      <Button
        disabled={isServiceRunning}
        title="Start Service"
        onPress={_startService}
      />
      <Button
        disabled={!isServiceRunning}
        title="Stop Service"
        onPress={_stopService}
      />
    </View>
  );
};

AppRegistry.registerComponent(appName, () => ListenMessageApp);
