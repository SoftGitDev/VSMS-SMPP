/*
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package com.softtech.smpp.session;

import java.io.IOException;

import com.softtech.smpp.bean.DeliverSm;
import com.softtech.smpp.bean.Outbind;
import com.softtech.smpp.extra.ProcessRequestException;

/**
 * <tt>OutboundServerResponseHandler</tt> provide interface to handle response of the session
 * routines.
 * 
 * @author SUTHAR
 * @version 1.0
 * @since 2.0
 * 
 */
public interface OutboundServerResponseHandler extends BaseResponseHandler {

    /**
     * Process the outbind
     *
     * @param outbind
     * @throws ProcessRequestException
     */
    void processOutbind(Outbind outbind)
        throws ProcessRequestException;

    /**
     * Process the deliver
     *
     * @param deliverSm
     * @throws ProcessRequestException
     */
    void processDeliverSm(DeliverSm deliverSm)
        throws ProcessRequestException;

    /**
     * Response by sending <b>DELIVER_SM_RESP</b> to SMSC.
     *
     * @param commandStatus is the command_status.
     * @param sequenceNumber is the sequence number of original <b>DELIVER_SM</b> request.
     * @param messageId is the message_id.
     * @throws IOException if an IO error occur.
     */
    void sendDeliverSmResp(int commandStatus, int sequenceNumber, String messageId) throws IOException;
}
