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

import com.softtech.smpp.bean.SubmitMultiResult;
import com.softtech.smpp.util.MessageId;

/**
 * It's abstract adapter class that receive event of response delivery, an
 * implementation of {@link ServerResponseDeliveryListener}.
 * <p>
 * This is alternative from implementing {@link ServerResponseDeliveryListener}.
 * User only have to create subclass of this class and doesn't have to implement
 * all method declared on {@link ServerResponseDeliveryListener}.
 * </p>
 * 
 * @author SUTHAR
 * 
 */
public abstract class ServerResponseDeliveryAdapter implements
        ServerResponseDeliveryListener {
    
    /* (non-Javadoc)
     * @see com.softtech.session.ServerResponseDeliveryListener#onSubmitSmResponseSent(com.softtech.util.MessageId, com.softtech.session.SMPPServerSession)
     */
    public void onSubmitSmRespSent(MessageId messageId,
            SMPPServerSession source) {}
    
    /* (non-Javadoc)
     * @see com.softtech.session.ServerResponseDeliveryListener#onSubmitSmResponseError(com.softtech.util.MessageId, java.lang.Exception, com.softtech.session.SMPPServerSession)
     */
    public void onSubmitSmRespError(MessageId messageId, Exception e,
            SMPPServerSession source) {}
    
    /* (non-Javadoc)
     * @see com.softtech.session.ServerResponseDeliveryListener#onSubmitMultiResponseSent(com.softtech.bean.SubmitMultiResult, com.softtech.session.SMPPServerSession)
     */
    public void onSubmitMultiRespSent(SubmitMultiResult submitMultiResult,
            SMPPServerSession source) {}
    
    /* (non-Javadoc)
     * @see com.softtech.session.ServerResponseDeliveryListener#onSubmitMultiResposnseError(com.softtech.bean.SubmitMultiResult, java.lang.Exception, com.softtech.session.SMPPServerSession)
     */
    public void onSubmitMultiRespError(
            SubmitMultiResult submitMultiResult, Exception e,
            SMPPServerSession source) {}
}
