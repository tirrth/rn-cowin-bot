import SmsListener from 'react-native-android-sms-listener';

module.exports = async taskData => {
  console.log(taskData);
  console.log('background');
  SmsListener.addListener(console.log);
};
