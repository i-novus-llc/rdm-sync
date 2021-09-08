package ru.i_novus.ms.rdm.sync.admin.n2o.operation;

import net.n2oapp.cache.template.CacheTemplate;
import net.n2oapp.criteria.dataset.DataSet;
import net.n2oapp.framework.api.metadata.SourceMetadata;
import net.n2oapp.framework.api.metadata.compile.BindProcessor;
import net.n2oapp.framework.api.metadata.compile.CompileContext;
import net.n2oapp.framework.api.metadata.compile.CompileProcessor;
import net.n2oapp.framework.api.metadata.validate.ValidateProcessor;
import net.n2oapp.framework.api.register.MetadataRegister;
import net.n2oapp.framework.config.compile.pipeline.operation.SourceCacheOperation;

import java.util.function.Supplier;

/**
 * Переопределение SourceCacheOperation,
 * чтобы не кешировать динамические метаданные syncMapping.
 */
public class RdmSyncSourceCacheOperation<S extends SourceMetadata> extends SourceCacheOperation<S> {

    public RdmSyncSourceCacheOperation() {
    }

    public RdmSyncSourceCacheOperation(CacheTemplate cacheTemplate, MetadataRegister metadataRegister) {
        super(cacheTemplate, metadataRegister);
    }

    @Override
    public S execute(CompileContext<?, ?> context, DataSet data, Supplier<S> supplier,
                     CompileProcessor compileProcessor,
                     BindProcessor bindProcessor,
                     ValidateProcessor validateProcessor) {
        String sourceId = context.getSourceId(bindProcessor);
        if (sourceId.startsWith("syncMapping")) {
            return supplier.get();
        }
        return super.execute(context, data, supplier, compileProcessor, bindProcessor, validateProcessor);
    }
}
