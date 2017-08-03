/*
 * Copyright 2017-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.abbie;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.TableModel;
import org.onosproject.ui.table.TableRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
//import java.io.InputStreamReader;
import java.util.Collection;

import java.io.BufferedReader;


/**
 * Skeletal ONOS UI Custom-View message handler.
 */
public class DpiUiMsgHandler extends UiMessageHandler {

    private static final String SAMPLE_CUSTOM_DATA_REQ = "sampleCustomDataRequest";
    private static final String SAMPLE_CUSTOM_DATA_RESP = "sampleCustomDataResponse";

    private static final String SAMPLE_CUSTOM_TABLE_REQ = "dpiDataRequest";
    private static final String SAMPLE_CUSTOM_TABLE_RESP = "dpiDataResponse";
    private static final String SAMPLE_CUSTOM = "dpis";

    private static final String NUMBER = "number";
    private static final String SQUARE = "square";
    private static final String CUBE = "cube";
    private static final String MESSAGE = "message";
    private static final String MSG_FORMAT = "Next incr!!!!!!!ememt is %d units";

    private static final String SRC = "src";
    private static final String ID = "id";
    private static final String DST = "dst";
    private static final String PROTOCOL = "protocol";
    private static final String LABEL = "label";
    private static final String HOSTLABEL = "hostLabel";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private long someNumber = 1;
    private long someIncrement = 1;

    private static final String[] COL_IDS = {
            ID, SRC, DST, PROTOCOL, LABEL, HOSTLABEL
    };

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new SampleCustomDataRequestHandler(),
                new SampleCustomTableRequestHandler()
        );
    }


    // handler for sample data requests
    private final class SampleCustomDataRequestHandler extends RequestHandler {

        private SampleCustomDataRequestHandler() {
            super(SAMPLE_CUSTOM_DATA_REQ);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            someIncrement++;
            someNumber += someIncrement;
            log.debug("Computing data for {}...", someNumber);

            ObjectNode result = objectNode();
            result.put(NUMBER, someNumber);
            result.put(SQUARE, someNumber * someNumber);
            result.put(CUBE, someNumber * someNumber * someNumber);
            result.put(MESSAGE, String.format(MSG_FORMAT, someIncrement + 1));
            sendMessage(SAMPLE_CUSTOM_DATA_RESP, 0, result);
        }
    }

    // handler for device table requests
    private final class SampleCustomTableRequestHandler extends TableRequestHandler {
        private static final String NO_ROWS_MESSAGE = "No DPI result";

        private SampleCustomTableRequestHandler() {
            super(SAMPLE_CUSTOM_TABLE_REQ, SAMPLE_CUSTOM_TABLE_RESP, SAMPLE_CUSTOM);
        }

        @Override
        protected String[] getColumnIds() {
            return COL_IDS;
        }

        @Override
        protected String noRowsMessage(ObjectNode payload) {
            return NO_ROWS_MESSAGE;
        }

        @Override
        protected void populateTable(TableModel tm, ObjectNode payload) {
            try {
                ClassLoader classLoader = getClass().getClassLoader();
                BufferedReader reader = new BufferedReader(
                        new FileReader("/home/mininet/abbie_dpi/temp.csv"));
                        //new InputStreamReader(
                                //classLoader.getResourceAsStream("temp.csv")));

                reader.readLine();
                String line = null;
                String[] item = null;
                String src = null;
                String dst = null;
                String protocol = null;
                String label = null;
                String hostLabel = null;

                int id = 1;
                while ((line = reader.readLine()) != null) {
                    item = line.split(","); //CSV

                    src = item[0];
                    dst = item[1];
                    protocol = item[2];
                    label = item[3];
                    if (label.equals("DNS") || label.equals("QUIC") || label.equals("HTTP")
                        || label.equals("SSL") || label.equals("DHCP") || label.equals("SSH")) {
                        if (item.length == 5) {
                            hostLabel = item[4];
                            populateRow(tm.addRow(), id, src, dst,
                                        protocol, label, hostLabel);
                        } else {
                            populateRow(tm.addRow(), id, src, dst,
                                        protocol, label, "");
                        }
                        id = id + 1;
                    } else if (label.equals("YouTube") || label.equals("NetFlix")
                                || label.equals("Facebook") || label.equals("Twitter")
                                || label.equals("Google") || label.equals("Github")) {
                        populateRow(tm.addRow(), id, src, dst, protocol, "", label);
                        id = id + 1;
                    }
                }

            } catch (Exception e) {
                log.info("Reader Error");
                e.printStackTrace();
            }
        }

        private void populateRow(TableModel.Row row,
                                 int id, String src, String dst,
                                 String protocol, String label, String hostLabel) {
            row.cell(ID, id)
                        .cell(SRC, src)
                        .cell(DST, dst)
                        .cell(PROTOCOL, protocol)
                        .cell(LABEL, label)
                        .cell(HOSTLABEL, hostLabel);
        }
    }

}
