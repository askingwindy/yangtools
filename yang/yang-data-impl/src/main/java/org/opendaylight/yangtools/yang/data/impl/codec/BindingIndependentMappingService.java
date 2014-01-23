package org.opendaylight.yangtools.yang.data.impl.codec;

import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;

public interface BindingIndependentMappingService {

    CodecRegistry getCodecRegistry();

    CompositeNode toDataDom(DataObject data);

    Entry<org.opendaylight.yangtools.yang.data.api.InstanceIdentifier, CompositeNode> toDataDom(
            Entry<InstanceIdentifier<? extends DataObject>, DataObject> entry);

    org.opendaylight.yangtools.yang.data.api.InstanceIdentifier toDataDom(InstanceIdentifier<? extends DataObject> path);

    DataObject dataObjectFromDataDom(InstanceIdentifier<? extends DataObject> path, CompositeNode result) throws DeserializationException;

    InstanceIdentifier<?> fromDataDom(org.opendaylight.yangtools.yang.data.api.InstanceIdentifier entry)  throws DeserializationException;

    Set<QName> getRpcQNamesFor(Class<? extends RpcService> service);

    DataContainer dataObjectFromDataDom(Class<? extends DataContainer> inputClass, CompositeNode domInput);

}
