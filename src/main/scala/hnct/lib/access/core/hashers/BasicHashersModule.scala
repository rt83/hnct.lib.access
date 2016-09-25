package hnct.lib.access.core.hashers

import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder
import hnct.lib.access.api.PasswordHasher
import com.google.inject.TypeLiteral

class BasicHashersModule extends AbstractModule {
	
	def configure() = {
		
		val sb = Multibinder.newSetBinder(binder(), new TypeLiteral[PasswordHasher[_,_]]() {})
		sb.addBinding().to(classOf[MD5Hasher])
		
	}
	
}