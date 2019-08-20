package com.gs.photo.workflow;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public interface IBeanTaskExecutor {

	public void execute(Runnable r);

	public <V> Collection<Future<V>> execute(Collection<Callable<V>> r);

}