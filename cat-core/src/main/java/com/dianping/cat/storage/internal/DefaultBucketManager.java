package com.dianping.cat.storage.internal;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.plexus.personality.plexus.lifecycle.phase.Disposable;

import com.dianping.cat.message.spi.MessageTree;
import com.dianping.cat.storage.Bucket;
import com.dianping.cat.storage.BucketManager;
import com.site.lookup.ContainerHolder;
import com.site.lookup.annotation.Inject;

public class DefaultBucketManager extends ContainerHolder implements BucketManager, Disposable {
	@Inject
	private String m_baseDir;

	private Map<Entry, Bucket<?>> m_map = new HashMap<Entry, Bucket<?>>();

	protected Bucket<?> createBucket(String path, Class<?> type, String namespace) throws IOException {
		Bucket<?> bucket;

		if (namespace.equals("hdfs")) {
			bucket = lookup(Bucket.class, "hdfs");
		}else if (namespace.equals("hdfs-logview")) {
			bucket = lookup(Bucket.class, "hdfs-logview");
		} else {
			bucket = lookup(Bucket.class, type.getName());
		}

		bucket.initialize(type, new File(m_baseDir), path);
		return bucket;
	}

	@Override
	public void dispose() {
		for (Bucket<?> bucket : m_map.values()) {
			release(bucket);
		}
	}

	@SuppressWarnings("unchecked")
	protected <T> Bucket<T> getBucket(Class<T> type, String path, String namespace) throws IOException {
		if (type == null || path == null) {
			throw new IllegalArgumentException(String.format("Type(%s) or path(%s) can't be null.", type, path));
		}

		Entry entry = new Entry(type, path, namespace);
		Bucket<?> bucket = m_map.get(entry);

		if (bucket == null) {
			synchronized (this) {
				bucket = m_map.get(entry);

				if (bucket == null) {
					bucket = createBucket(path, type, namespace);
					m_map.put(entry, bucket);
				}
			}
		}

		return (Bucket<T>) bucket;
	}

	@Override
	public Bucket<byte[]> getBytesBucket(String path) throws IOException {
		return getBucket(byte[].class, path, "file");
	}

	@Override
	public Bucket<byte[]> getHdfsBucket(String path) throws IOException {
		return getBucket(byte[].class, path, "hdfs");
	}

	@Override
	public Bucket<MessageTree> getMessageBucket(String path) throws IOException {
		return getBucket(MessageTree.class, path, "file");
	}

	@Override
	public Bucket<String> getStringBucket(String path) throws IOException {
		return getBucket(String.class, path, "file");
	}

	public void setBaseDir(String baseDir) {
		m_baseDir = baseDir;
	}

	static class Entry {
		private Class<?> m_type;

		private String m_path;

		private String m_namespace;

		public Entry(Class<?> type, String path, String namespace) {
			m_type = type;
			m_path = path;
			m_namespace = namespace;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Entry) {
				Entry e = (Entry) obj;

				return e.getType() == m_type && e.getPath().equals(m_path) && e.getNamespace().equals(m_namespace);
			}

			return false;
		}

		public String getPath() {
			return m_path;
		}

		public Class<?> getType() {
			return m_type;
		}

		public String getNamespace() {
			return m_namespace;
		}

		@Override
		public int hashCode() {
			int hashcode = m_type.hashCode();

			hashcode = hashcode * 31 + m_path.hashCode();
			hashcode = hashcode * 31 + m_namespace.hashCode();
			return hashcode;
		}
	}

	@Override
	public void closeBucket(Bucket<?> bucket) {
		try {
			bucket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		release(bucket);
	}
}
