/**
 * @format
 */

import {AppRegistry, Button, Text, View, NativeModules} from 'react-native';
import {name as appName} from './app.json';
import React, {Component} from 'react';
// import BackgroundJob from 'react-native-background-job';
// import SmsListener from 'react-native-android-sms-listener';

// const MessageListener = async taskData => {
//   console.log(taskData);
//   console.log('background');
//   SmsListener.addListener(console.log);
// };

// AppRegistry.registerHeadlessTask('MessageListener', () => MessageListener);

/*
Register background job with jobKey
*/
// const backgroundJob = {
//   jobKey: 'myJob',
//   job: () => console.log('Running in background'),
// };

// BackgroundJob.register(backgroundJob);

// const backgroundSchedule = {jobKey: 'myJob'};
// BackgroundJob.schedule(backgroundSchedule)
//   .then(() => console.log('Success'))
//   .catch(err => console.err(err));

const {ServiceModule} = NativeModules;
export default class ListenMessageApp extends Component {
  //constructor include last message
  constructor(props) {
    super(props);
    this.state = {lastMessage: 1};
  }

  componentDidMount() {
    // this.getAll();
    // BackgroundJob.schedule({jobKey: myJobKey});
  }

  //Schedule function in background job

  getAll() {
    // BackgroundJob.getAll({
    //   callback: () => {
    // SmsListener.addListener(message => {
    //   console.log('foreground');
    //   console.log(message);
    //   if (message.originatingAddress === 'VD-NHPSMS') {
    //     this.setState({lastMessage: message.body});
    //   }
    // });
    //   },
    // });
  }

  render() {
    const {startService, stopService} = ServiceModule;
    return (
      <View>
        <Text> Scheduled jobs: {this.state.lastMessage} </Text>
        <Button title="Start Service" onPress={() => startService()} />
        <Button title="Stop Service" onPress={() => stopService()} />
      </View>
    );
  }
}

AppRegistry.registerComponent(appName, () => ListenMessageApp);
