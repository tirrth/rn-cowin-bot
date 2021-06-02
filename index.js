/**
 * @format
 */

import {AppRegistry, Button, Text, View, NativeModules} from 'react-native';
import {name as appName} from './app.json';
import React, {Component} from 'react';

const {ServiceModule} = NativeModules;
export default class ListenMessageApp extends Component {
  render() {
    const {startService, stopService} = ServiceModule;
    return (
      <View>
        <Button title="Start Service" onPress={() => startService()} />
        <Button title="Stop Service" onPress={() => stopService()} />
      </View>
    );
  }
}

AppRegistry.registerComponent(appName, () => ListenMessageApp);
