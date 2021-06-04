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
      refresh_interval: '5',
      pincodes: ['382350', '382345'],
      district_ids: [],
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
