package listener;

import br.org.eldorado.sensorapi.model.SensorBase;

public interface SensorListener {

	public void onStarted(SensorBase s);
	public void onStopped(SensorBase s);
	public void onChanged(SensorBase s);
}
