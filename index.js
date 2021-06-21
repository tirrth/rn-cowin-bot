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
import {name as appName} from './app.json';
import React, {useEffect} from 'react';
const {ServiceModule} = NativeModules;

const requestPermission = async () => {
  try {
    const granted = await PermissionsAndroid.requestMultiple([
      PermissionsAndroid.PERMISSIONS.READ_PHONE_STATE,
      PermissionsAndroid.PERMISSIONS.RECEIVE_SMS,
    ]);
    if (granted === PermissionsAndroid.RESULTS.GRANTED) {
      console.log('Permission Accepted');
    } else {
      console.log('Permission denied');
    }
  } catch (err) {
    console.warn(err);
  }
};

const ListenMessageApp = () => {
  const {startService, stopService} = ServiceModule;

  useEffect(() => {
    (async () => {
      await requestPermission();
    })();
  }, []);

  const _startService = () => {
    startService({
      access_token:
        'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX25hbWUiOiI1ZGY3YjE4MC02YTcyLTRiOTYtYWQxNy01ZmE0MjcwMTVmY2EiLCJ1c2VyX3R5cGUiOiJCRU5FRklDSUFSWSIsInVzZXJfaWQiOiI1ZGY3YjE4MC02YTcyLTRiOTYtYWQxNy01ZmE0MjcwMTVmY2EiLCJtb2JpbGVfbnVtYmVyIjo5MTA2MTMyODcwLCJiZW5lZmljaWFyeV9yZWZlcmVuY2VfaWQiOjg0NDgxNjAyOTMzNDEwLCJ0eG5JZCI6ImNkODdmMzUxLTdlOTktNGFhZC1hMGY3LTc4OTFmMTQ2OGFiMyIsImlhdCI6MTYyMjgyOTcwMSwiZXhwIjoxNjIyODMwNjAxfQ.gLYz5FGqMss0md0dANQTXoqJhAmAxls0TwnJBc46XQA',
      mobile: '9106132870',
      refresh_interval: '3',
      min_age_limit: '18',
      pincodes: ['382350', '382345', '380050', '380045', '382323'],
      district_ids: ['154', '174', '158', '175', '181'],
    });
  };

  const _stopService = () => {
    stopService();
  };

  return (
    <View>
      <Button
        // disabled={isServiceRunning}
        title="Start Service"
        onPress={_startService}
      />
      <Button
        // disabled={!isServiceRunning}
        title="Stop Service"
        onPress={_stopService}
      />
    </View>
  );
};

AppRegistry.registerComponent(appName, () => ListenMessageApp);
