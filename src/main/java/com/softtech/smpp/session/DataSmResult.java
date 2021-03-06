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

import com.softtech.smpp.bean.OptionalParameter;
import com.softtech.smpp.util.MessageId;

/**
 * @author SUTHAR
 *
 */
public class DataSmResult {
    private final String messageId;
    private final OptionalParameter[] optionalParameters;
    
    
    DataSmResult(String messageId, OptionalParameter[] optionalParameters) {
        this.messageId = messageId;
        this.optionalParameters = optionalParameters;
    }
    
    public DataSmResult(MessageId messageId, OptionalParameter[] optionalParameters) {
        this(messageId.getValue(), optionalParameters);
    }

    public String getMessageId() {
        return messageId;
    }

    public OptionalParameter[] getOptionalParameters() {
        return optionalParameters;
    }
}
