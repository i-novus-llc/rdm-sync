package ru.i_novus.ms.rdm.sync.admin.n2o.operation;

import net.n2oapp.cache.template.CacheTemplate;
import net.n2oapp.criteria.dataset.DataSet;
import net.n2oapp.framework.api.metadata.compile.BindProcessor;
import net.n2oapp.framework.api.metadata.compile.CompileContext;
import net.n2oapp.framework.api.metadata.compile.CompileProcessor;
import net.n2oapp.framework.api.metadata.validate.ValidateProcessor;
import net.n2oapp.framework.config.compile.pipeline.operation.CompileCacheOperation;

import java.util.function.Supplier;

/**
 * Переопределение CompileCacheOperation,
 * чтобы не кешировать динамические метаданные syncMapping.
 */
public class RdmSyncCompileCacheOperation<S> extends CompileCacheOperation<S> {

    public RdmSyncCompileCacheOperation() {
    }

    public RdmSyncCompileCacheOperation(CacheTemplate cacheTemplate) {
        super(cacheTemplate);
    }

    @Override
    public S execute(CompileContext<?,?> context, DataSet data, Supplier<S> supplier,
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
