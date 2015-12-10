/*
 * Copyright (C) 2015 Karumi.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.karumi.rosie.view;

import com.karumi.rosie.domain.usecase.RosieUseCase;
import com.karumi.rosie.domain.usecase.UseCaseHandler;
import com.karumi.rosie.domain.usecase.UseCaseParams;
import com.karumi.rosie.domain.usecase.callback.OnSuccessCallback;
import com.karumi.rosie.domain.usecase.error.OnErrorCallback;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedList;
import java.util.List;

/**
 * Implements all the presentation logic. All Presenters must extends from this class and indicate
 * which view extending from RosiePresenter.View interface are going to use. Override lifecycle
 * methods to be able to react to the view lifecycle.
 */
public class RosiePresenter<T extends RosiePresenter.View> {

  private final UseCaseHandler useCaseHandler;
  private final List<OnSuccessCallback> useCaseOnSuccessCallbacks = new LinkedList<>();
  private final List<OnErrorCallback> useCaseOnErrorCallbacks = new LinkedList<>();
  private final List<OnErrorCallback> globalOnErrorCallbacks = new LinkedList<>();
  private T view;
  private boolean shouldRegisterGlobalErrorCallbacks = true;

  public RosiePresenter(UseCaseHandler useCaseHandler) {
    this.useCaseHandler = useCaseHandler;
  }

  /**
   * Method called in the presenter lifecycle. Invoked when the component containing the
   * presenter is initialized.
   */
  protected void initialize() {
    registerGlobalErrorCallback();
  }

  /**
   * Method called in the presenter lifecycle. Invoked when the component containing the
   * presenter is resumed.
   */
  protected void update() {
    registerGlobalErrorCallback();
  }

  /**
   * Method called in the presenter lifecycle. Invoked when the component containing the
   * presenter is paused.
   */
  protected void pause() {
    unregisterGlobalErrorCallback();
  }

  /**
   * Method called in the presenter lifecycle. Invoked when the component containing the
   * presenter is destroyed.
   */
  protected void destroy() {
    globalOnErrorCallbacks.clear();
  }

  /**
   * Executes a RosieUseCase passed as parameter using the UseCaseHandler instance obtained during
   * the RosiePresenter construction.
   */
  protected final void execute(RosieUseCase useCase) {
    useCaseHandler.execute(useCase);
  }

  /**
   * Executes a RosieUseCase passed as parameter using the UseCaseHandler instance obtained during
   * the RosiePresenter construction and the UseCaseParams object passed as second parameter.
   *
   * This method also keeps a strong reference of OnSuccessCallback and OnErrorCallback parameters
   * because most of the times this method is called using anonymous functions as callbacks
   */
  protected final void execute(RosieUseCase useCase, UseCaseParams useCaseParams) {
    retainCallbackReferences(useCaseParams);
    useCaseHandler.execute(useCase, useCaseParams);
  }

  /**
   * Returns the view configured in the presenter which real implementation is an Activity or
   * Fragment using this presenter.
   */
  protected final T getView() {
    return view;
  }

  /**
   * Returns the UseCaseHandler instance used to create this presenter class.
   */
  protected UseCaseHandler getUseCaseHandler() {
    return useCaseHandler;
  }

  /**
   * Registers a global callback for all the use cases executed from this presenter. Global error
   * callbacks need to be registered in your constructor.
   *
   * @param onErrorCallback The callback being registered
   */
  protected void registerOnErrorCallback(OnErrorCallback onErrorCallback) {
    globalOnErrorCallbacks.add(onErrorCallback);
  }

  /**
   * Configures the View instance used in this presenter as view.
   */
  void setView(T view) {
    this.view = view;
  }

  /**
   * Changes the current view instance with a dynamic proxy to avoid real UI updates.
   */
  void resetView() {
    final Class<?> viewClass = getViewInterfaceClass();
    InvocationHandler emptyHandler = new InvocationHandler() {
      @Override public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return null;
      }
    };
    ClassLoader classLoader = viewClass.getClassLoader();
    Class[] interfaces = new Class[1];
    interfaces[0] = viewClass;
    this.view = (T) Proxy.newProxyInstance(classLoader, interfaces, emptyHandler);
  }

  private Class<?> getViewInterfaceClass() {
    Class<?> interfaceClass = null;
    Class<?>[] interfaces = this.view.getClass().getInterfaces();
    for (int i = 0; i < interfaces.length; i++) {
      Class<?> interfaceCandidate = interfaces[i];
      if (RosiePresenter.View.class.isAssignableFrom(interfaceCandidate)) {
        interfaceClass = interfaceCandidate;
      }
    }
    return interfaceClass;
  }

  private void registerGlobalErrorCallback() {
    if (shouldRegisterGlobalErrorCallbacks) {
      for (OnErrorCallback onErrorCallback : globalOnErrorCallbacks) {
        useCaseHandler.registerGlobalErrorCallback(onErrorCallback);
      }
      shouldRegisterGlobalErrorCallbacks = false;
    }
  }

  private void unregisterGlobalErrorCallback() {
    for (OnErrorCallback onErrorCallback : globalOnErrorCallbacks) {
      useCaseHandler.unregisterGlobalErrorCallback(onErrorCallback);
    }
    shouldRegisterGlobalErrorCallbacks = true;
  }

  /**
   * Represents the View component inside the Model View Presenter pattern. This interface must be
   * used as base interface for every View interface declared.
   */
  private void retainCallbackReferences(UseCaseParams useCaseParams) {
    OnSuccessCallback onSuccessCallback = useCaseParams.getOnSuccessCallback();
    if (onSuccessCallback != null) {
      useCaseOnSuccessCallbacks.add(onSuccessCallback);
    }
    OnErrorCallback onErrorCallback = useCaseParams.getOnErrorCallback();
    if (onErrorCallback != null) {
      useCaseOnErrorCallbacks.add(onErrorCallback);
    }
  }

  public interface View {

  }
}