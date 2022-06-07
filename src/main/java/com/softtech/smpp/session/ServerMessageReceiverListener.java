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

import com.softtech.smpp.bean.CancelSm;
import com.softtech.smpp.bean.QuerySm;
import com.softtech.smpp.bean.ReplaceSm;
import com.softtech.smpp.bean.SubmitMulti;
import com.softtech.smpp.bean.SubmitMultiResult;
import com.softtech.smpp.bean.SubmitSm;
import com.softtech.smpp.extra.ProcessRequestException;
import com.softtech.smpp.util.MessageId;

/**
 * @author SUTHAR
 *
 */
public interface ServerMessageReceiverListener extends GenericMessageReceiverListener {
    MessageId onAcceptSubmitSm(SubmitSm submitSm, SMPPServerSession source)
            throws ProcessRequestException;
    
    SubmitMultiResult onAcceptSubmitMulti(SubmitMulti submitMulti,
            SMPPServerSession source) throws ProcessRequestException;
    
    QuerySmResult onAcceptQuerySm(QuerySm querySm, SMPPServerSession source)
            throws ProcessRequestException;
    
    void onAcceptReplaceSm(ReplaceSm replaceSm, SMPPServerSession source)
            throws ProcessRequestException;
    
    void onAcceptCancelSm(CancelSm cancelSm, SMPPServerSession source)
            throws ProcessRequestException; 
}
