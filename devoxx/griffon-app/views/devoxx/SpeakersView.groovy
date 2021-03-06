/*
 * Copyright (c) 2010 Devoxx Schedule app - Andres Almiray. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  o Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  o Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  o Neither the name of Effects - Andres Almiray nor the names of
 *    its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 'AS IS'
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package devoxx

import ca.odell.glazedlists.*
import ca.odell.glazedlists.gui.*
import ca.odell.glazedlists.swing.*
import ca.odell.glazedlists.event.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentListener

/**
 * @author Andres Almiray
 */

def type = Constants.TYPES.speakers

speakersMatcherEditor = new TextComponentMatcherEditor(
   searchField(id: 'speakerSearch'),
   {List baseList, info ->
      baseList << info.firstName
      baseList << info.lastName
      baseList << info.company
   } as TextFilterator
)

filteredSpeakers = new FilterList(model.speakers, speakersMatcherEditor)
filteredSpeakers.addListEventListener({ e ->
    model.size = e.sourceList.size()
} as ListEventListener)

speakersTrackingSelectionModel = bean(new EventSelectionModel(filteredSpeakers),
   selectionMode: EventSelectionModel.SINGLE_SELECTION,)

def createSpeakersTableModel() {
   new EventTableModel(filteredSpeakers, [
        getColumnCount: { 1i },
        getColumnName: {index -> ''},
        getColumnValue: {object, index -> object}
    ] as TableFormat)
}

def createSpeakersTableModel_copy() {
   def columnNames = ['First Name', 'Last Name', 'Company']
   def propertyNames = ['firstName', 'lastName', 'company']
   new EventTableModel(filteredSpeakers, [
        getColumnCount: { columnNames.size() },
        getColumnName: {index -> columnNames[index]},
        getColumnValue: {object, index -> object."${propertyNames[index]}"}
    ] as TableFormat)
}

// Usage of a noparent{} block avoids inserting a Swing component
// in the current hierarchy
noparent {
    // Create a copy of the real table but with a different TableFormat
    // this will alow the usage of a multi-column JTableHeader with a
    // single column model
    table(id: 'speakersTable_copy', model: createSpeakersTableModel_copy())
}

panel(id: 'box', opaque: false) {
    migLayout(layoutConstraints: 'insets 0 0 0 0, fill')
    label(icon: crystalIcon(size: 22, category: type.icon.category, icon: type.icon.name),
          constraints: 'left, top, grow',
          text: bind('size', source: model, converter: {v -> "${type.description} (${v})".toString()}))
    widget(speakerSearch, columns: 20, constraints: 'right, top, wrap')
    // add the multi-column header first
    widget(speakersTable_copy.tableHeader, constraints: 'span 2, top, growx, gap bottom 0, wrap',
           resizingAllowed: false, reorderingAllowed: false)
    // then comes the real table
    scrollPane(id: 'speakersTableContainer', opaque: false, constraints: 'span 2, top, grow') {
        table(id: 'speakersTable', model: createSpeakersTableModel(),
              selectionModel: speakersTrackingSelectionModel) {
            def column = speakersTable.columnModel.getColumn(0i)
            column.cellRenderer = new SpeakerTableCellRenderer(delegate)
        }
    }

    noparent {
        // further customizations to the multi-column header
        // 1st we add sorting capabilities
        TableComparatorChooser.install(speakersTable_copy, model.speakers,
            TableComparatorChooser.SINGLE_COLUMN)
        // 2nd sincronize its size with the table container
        speakersTableContainer.addComponentListener([
            componentResized: { e ->
                def tableHeader = speakersTable_copy.tableHeader
                int totalWidth = e.source.size.width
                int currentHeight = tableHeader.size.height

                // update tableHeader size
                tableHeader.setSize(totalWidth as int, currentHeight as int)
                int columnWidth = totalWidth / tableHeader.columnModel.columnCount
                // update each column too
                for(column in tableHeader.columnModel.columns) {
                    column.width = columnWidth as int
                    column.preferredWidth = columnWidth as int
                }
            }] as ComponentAdapter)
    }
}
