import React from 'react'
import {
  StyleSheet,
  Text,
  ListView,
  TouchableHighlight,
} from 'react-native'

export default class UpdateableList extends React.Component {
  constructor(props) {
    super(props)
    let dataSource = new ListView.DataSource({
      rowHasChanged: (a, b) => a !== b,
    })
    // this.pageSize = props.pageSize // also set this.pageSize in prefetchStarted - so we know how much items should be in this.page array
    // this.page = [] // for prefetch, doesn't used in UpdateableList ^_^
    // for ImageList in prefetch.Resolved: state.page = elements for current page, after page.length === pageSize
    // for ImageList on prefetch.Rejected: just ignore and decrement this.pageSize
    this.rows = props.newRows.splice(0, props.pageSize)
    this.state = {
      newRows: props.newRows, // in state, coz used for renderFooter assignment expression
      dataSource: dataSource.cloneWithRows(this.rows),
    }
    
    this._renderFooter = this._renderFooter.bind(this)
    this._onPressMore = this._onPressMore.bind(this)
  }

  _renderFooter() { // if state.loading (<ActivityIndicator/>) else (<TouchableHighlight/>)
    return (
      <TouchableHighlight
        underlayColor='#C8C7CC'
        onPress={this._onPressMore}
      >
        <Text style={styles.touchableText}>
          Load more    
        </Text>
      </TouchableHighlight>
    )
  }

  _onPressMore() {
    this.rows = this.rows.concat(this.state.newRows.splice(0, this.props.pageSize))
    this.setState({
      newRows: this.state.newRows, // ^_^
      dataSource: this.state.dataSource.cloneWithRows(this.rows),
    })
  }

  render() {
    return (
      <ListView style={styles.list}
        dataSource={this.state.dataSource}
        renderRow={this.props.renderRow}
        renderFooter={this.state.newRows.length > 0 ? this._renderFooter : null}
      />
    )
  }
}

/**
 * type ImageListItem {
 *  url: string,
 *  ...any
 * }
 * rows: ImageListItem[]
 */
UpdateableList.props = {
  newRows: React.PropTypes.array,
  pageSize: React.PropTypes.number.isRequired,
  renderRow: React.PropTypes.func.isRequired,
}

const styles = StyleSheet.create({
  list: {
    flex: 1,
    flexDirection: 'column',
    // justifyContent: 'center', // node_modules/react-native/Libraries/Components/ScrollView/ScrollView.js:496: ScrollView child layout ('alignItems', 'justifyContent') must be applied through the contentContainerStyle prop.
    // alignItems: 'center',
    backgroundColor: '#FFFFFF',
  },
  container: {
    flex: 1,
    flexDirection: 'column',
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#FFFFFF',
  },
  touchableText: {
    fontSize: 20,
    margin: 10,
    textAlign: 'center',
    color: '#7BAAF7',
  },
})
