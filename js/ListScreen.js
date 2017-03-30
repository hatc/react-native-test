import React from 'react'
import {
  StyleSheet,
  Text,
  Image,
  View,
} from 'react-native'

import UpdateableList from './UpdateableList'

export default class ListScreen extends React.Component {
  constructor(props) {
    super(props)

    this._renderRow = this._renderRow.bind(this)
  }

  _renderRow(rowData, sectionID, rowID, highlightRow) {
    // url name
    return (
      <View>
        <Image
          style={styles.img}
          source={{uri: rowData.url}}
        >
        </Image>
        <Text style={styles.text}>
          {rowData.name}
        </Text>
      </View>
    )
  }

  render() {
    return (
      <UpdateableList
        newRows={this.props.newRows}
        pageSize={3}
        renderRow={this._renderRow}
      />
    )
  }
}

ListScreen.props = {
  newRows: React.PropTypes.array,
}

const styles = StyleSheet.create({
  img: {
    height: 200,
    resizeMode: 'contain',
  },
  text: {
    fontSize: 20,
    textAlign: 'center',
    color: '#333333',
    margin: 10,
  },
})
