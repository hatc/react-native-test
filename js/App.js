// https://github.com/react-community/react-navigation/blob/master/examples/NavigationPlayground/js/App.js

import React from 'react'
import {
  DeviceEventEmitter,
  NativeModules,
  ActivityIndicator,
  View,
  StyleSheet,
} from 'react-native'

import ListScreen from './ListScreen'

export default class App extends React.Component {
  constructor(props) {
    super(props)

    this.subscription = null
    this.state = {
      newRows: [],
      loading: false,
    }

    this._onEvent = this._onEvent.bind(this)
  }

  _getPhotos(tag) {
    this.setState({
      loading: true,
    })

    NativeModules.PromisingDelegate.invokeDelegate('getPhotos', { 'tag': tag }).then(
      photos => { this.setState({
        loading: false,
        newRows: photos // photos.map(v => v.url)
      })
      },
      reason => { console.error(reason) }
    )

    this.forceUpdate()
  }

  _onEvent(e) {
    // console.warn('property: "' + e.property + '", value: "' + e.value + '"')
    if (e.property === 'tag') {
      this._getPhotos(e.value)
    }
  }
  componentDidMount() {
    this.subscription = DeviceEventEmitter.addListener('com.basictest.MainModel', this._onEvent)
  }
  componentWillUnmount() {
    if (this.subscription !== null) {
      this.subscription.remove()
    }
  }

  render() {
    // const loading = this.state.loading
    return (
      <View style={styles.container}>
      {this.state.loading ? (
      <ActivityIndicator/>
      ) : (
      <ListScreen
        newRows={this.state.newRows}
        style={styles.container}
      />)}
      </View>
    )
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#FFFFFF',
  },
})
