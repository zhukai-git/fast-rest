package com.zhukai.framework.fast.rest.bean.component;

import com.zhukai.framework.fast.rest.bean.BeanFactory;
import com.zhukai.framework.fast.rest.bean.ChildBean;
import com.zhukai.framework.fast.rest.proxy.ProxyFactory;
import com.zhukai.framework.fast.rest.util.ReflectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ComponentBeanFactory implements BeanFactory<ComponentBean> {

	private static final Logger logger = LoggerFactory.getLogger(ComponentBeanFactory.class);

	private static ComponentBeanFactory instance = new ComponentBeanFactory();
	private final Map<String, ComponentBean> componentBeanMap = Collections.synchronizedMap(new HashMap<>());
	private final Map<String, Object> singletonBeanMap = Collections.synchronizedMap(new HashMap<>());

	public static ComponentBeanFactory getInstance() {
		return instance;
	}

	private ComponentBeanFactory() {
	}

	@Override
	public Object getBean(String beanName) {
		if (!componentBeanMap.containsKey(beanName)) {
			logger.warn("ComponentBeanFactory not exits {}", beanName);
			return null;
		}
		ComponentBean componentBean = componentBeanMap.get(beanName);
		Class beanClass = componentBean.getBeanClass();
		Object object;
		if (componentBean.isSingleton()) {
			if (singletonBeanMap.containsKey(beanName)) {
				return singletonBeanMap.get(beanName);
			} else {
				object = ProxyFactory.createInstance(beanClass);
				singletonBeanMap.put(beanName, object);
			}
		} else {
			object = ProxyFactory.createInstance(beanClass);
		}
		for (ChildBean childBean : componentBean.getChildren()) {
			ReflectUtil.setFieldValue(object, childBean.getFieldName(), childBean.getBeanFactory().getBean(childBean.getRegisterName()));
		}
		return object;
	}

	@Override
	public boolean containsBean(String name) {
		return componentBeanMap.containsKey(name);
	}

	@Override
	public void registerBean(ComponentBean componentBean) {
		if (!componentBeanMap.containsKey(componentBean.getRegisterName())) {
			componentBeanMap.put(componentBean.getRegisterName(), componentBean);
			logger.info("Register in componentBeanFactory: {} = {}.class", componentBean.getRegisterName(), componentBean.getBeanClass().getSimpleName());
		}
	}

}
