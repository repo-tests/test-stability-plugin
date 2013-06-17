package de.esailors.jenkins.teststability;

import hudson.tasks.junit.TestAction;
import hudson.tasks.junit.TestObject;
import hudson.tasks.junit.TestResultAction.Data;
import hudson.tasks.junit.CaseResult;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import jenkins.model.Jenkins;

class StabilityTestData extends Data {
	
	private final Map<String,CircularStabilityHistory> stability;
	
	public StabilityTestData(Map<String, CircularStabilityHistory> stabilityHistory) {
		this.stability = stabilityHistory;
	}

	@SuppressWarnings("deprecation")
	@Override
	public List<? extends TestAction> getTestAction(TestObject testObject) {
		
		if (testObject instanceof CaseResult) {
			CaseResult cr = (CaseResult) testObject;
			CircularStabilityHistory ringBuffer = stability.get(cr.getId());
			return Collections.singletonList(new StabilityTestAction(ringBuffer));
		}
		
		return Collections.emptyList();
	}
	
	
	
	public static class CircularStabilityHistory {
		
		  private transient Result[] data;
		  private int head; 
		  private int tail;
		  // number of elements in queue
	      private int size = 0; 

	      private CircularStabilityHistory() {}
	      
		  public CircularStabilityHistory(int maxSize) {
		    data = new Result[maxSize];
		    head = 0;
		    tail = 0;
		  }

		  public boolean add(Result value) {
		      data[tail] = value;
		      tail++;
		      if (tail == data.length) {
		        tail = 0;
		      }
		      
		      if (size == data.length) {  
	                head = (head + 1) % data.length;  
	           } else {  
	                size++;  
	           }  
		      return true;
		  }
		  
		  public Result[] getData() {
			  Result[] copy = new Result[size];
			  
			  for (int i = 0; i < size; i++) {
				  copy[i] = data[(head + i) % data.length];
			  }
			  return copy;
		  }

		public boolean isEmpty() {
			return data.length == 0;
		}
		
		public int getMaxSize() {
			return this.data.length;
		}
		
		static {
			Jenkins.XSTREAM2.registerConverter(new ConverterImpl());
		}
		
		public static class ConverterImpl implements Converter {

			public boolean canConvert(@SuppressWarnings("rawtypes") Class type) {
				return CircularStabilityHistory.class.isAssignableFrom(type);
			}

			public void marshal(Object source, HierarchicalStreamWriter writer,
					MarshallingContext context) {
				CircularStabilityHistory b = (CircularStabilityHistory) source;
				
				writer.startNode("head");
				writer.setValue(Integer.toString(b.head));
				writer.endNode();
				
				writer.startNode("tail");
				writer.setValue(Integer.toString(b.tail));
				writer.endNode();

				writer.startNode("size");
				writer.setValue(Integer.toString(b.size));
				writer.endNode();
				
				writer.startNode("data");
				writer.setValue(dataToString(b.data));
				writer.endNode();
			}
			
			
			private String dataToString(Result[] data) {
				StringBuilder buf = new StringBuilder();
				for (Result d : data) {
					if(d == null) {
						buf.append(",");
						continue;
					}
					if (d.passed) {
						buf.append(d.buildNumber).append(";").append("1,");
					} else {
						buf.append(d.buildNumber).append(";").append("0,");
					}
				}
				
				if (buf.length() > 0) {
					buf.deleteCharAt(buf.length() - 1);
				}
				
				return buf.toString();
			}

			public CircularStabilityHistory unmarshal(HierarchicalStreamReader r,
					UnmarshallingContext context) {
				
				r.moveDown();
				int head = Integer.parseInt(r.getValue());
				r.moveUp();
				
				r.moveDown();
				int tail = Integer.parseInt(r.getValue());
				r.moveUp();
				
				r.moveDown();
				int size = Integer.parseInt(r.getValue());
				r.moveUp();
				
				r.moveDown();
				String data = r.getValue();
				r.moveUp();
				
				CircularStabilityHistory buf = new CircularStabilityHistory();
				Result[] b = stringToData(data);
				
				buf.data = b;
				buf.head = head;
				buf.size = size;
				buf.tail = tail;
				
				return buf;
			}
			
			private  Result[] stringToData(String s) {
				String[] split = s.split(",", -1);
				Result d[] = new Result[split.length];
				
				int i = 0;
				for(String testResult : split) {
					
					if (testResult.isEmpty()) {
						i++;
						continue;
					}
					
					String[] split2 = testResult.split(";");
					int buildNumber = Integer.parseInt(split2[0]);
					
					// TODO: check that '0' is the only other allowed value:
					boolean buildResult = "1".equals(split2[1]) ? true : false;
					
					d[i] = new Result(buildNumber, buildResult);
					
					i++;
				}
				
				return d;
			}

		}

		public void addAll(Result[] results) {
			for (Result b : results) {
				add(b);
			}
		}

		public void add(int number, boolean passed) {
			add(new Result(number, passed));
		}
		
	}
	
	public static class Result {
		int buildNumber;
		boolean passed;
		
		public Result(int buildNumber, boolean passed) {
			super();
			this.buildNumber = buildNumber;
			this.passed = passed;
		}
	}
	
	
}
