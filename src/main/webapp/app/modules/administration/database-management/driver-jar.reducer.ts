import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';

import { IDriverJar } from './driver-jar.model';

const initialState = {
  loading: false,
  errorMessage: null,
  driverJars: [] as ReadonlyArray<IDriverJar>,
  driverJar: null as IDriverJar | null,
  activeDriverJars: [] as ReadonlyArray<IDriverJar>,
};

export type DriverJarManagementState = Readonly<typeof initialState>;

// Actions
export const getDriverJars = createAsyncThunk('driverJarManagement/fetch_all', async () => {
  const requestUrl = 'api/database-drivers';
  return axios.get<IDriverJar[]>(requestUrl);
});

export const getDriverJar = createAsyncThunk('driverJarManagement/fetch_one', async (id: string | number) => {
  const requestUrl = `api/database-drivers/${id}`;
  return axios.get<IDriverJar>(requestUrl);
});

export const getDriverJarsByVersionId = createAsyncThunk('driverJarManagement/fetch_by_version', async (versionId: string | number) => {
  const requestUrl = `api/database-drivers?versionId=${versionId}`;
  return axios.get<IDriverJar[]>(requestUrl);
});

export const uploadDriverJar = createAsyncThunk(
  'driverJarManagement/upload',
  async (params: { versionId: number; driverType: string; driverClassName: string; file: File; description?: string }) => {
    const formData = new FormData();
    formData.append('versionId', params.versionId.toString());
    formData.append('driverType', params.driverType);
    formData.append('driverClassName', params.driverClassName);
    formData.append('file', params.file);
    if (params.description) {
      formData.append('description', params.description);
    }

    const result = await axios.post<IDriverJar>('api/database-drivers/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return result;
  },
);

export const updateDriverJar = createAsyncThunk('driverJarManagement/update', async (driverJar: IDriverJar) => {
  const result = await axios.put<IDriverJar>(`api/database-drivers/${driverJar.id}`, driverJar);
  return result;
});

export const deleteDriverJar = createAsyncThunk('driverJarManagement/delete', async (id: string | number) => {
  const requestUrl = `api/database-drivers/${id}`;
  await axios.delete(requestUrl);
  return id;
});

export const DriverJarManagementSlice = createSlice({
  name: 'driverJarManagement',
  initialState: initialState as DriverJarManagementState,
  reducers: {
    reset() {
      return initialState;
    },
  },
  extraReducers(builder) {
    builder
      .addCase(getDriverJars.pending, state => {
        state.loading = true;
      })
      .addCase(getDriverJars.fulfilled, (state, action) => {
        state.loading = false;
        const drivers = action.payload.data;
        state.driverJars = drivers;
        state.activeDriverJars = drivers.filter(d => d.active !== false);
      })
      .addCase(getDriverJars.rejected, (state, action) => {
        state.loading = false;
        state.errorMessage = action.error.message;
      })
      .addCase(getDriverJar.pending, state => {
        state.loading = true;
      })
      .addCase(getDriverJar.fulfilled, (state, action) => {
        state.loading = false;
        state.driverJar = action.payload.data;
      })
      .addCase(getDriverJar.rejected, (state, action) => {
        state.loading = false;
        state.errorMessage = action.error.message;
      })
      .addCase(getDriverJarsByVersionId.fulfilled, (state, action) => {
        state.loading = false;
        const drivers = action.payload.data;
        state.driverJars = drivers;
        state.activeDriverJars = drivers.filter(d => d.active !== false);
      })
      .addCase(uploadDriverJar.pending, state => {
        state.loading = true;
      })
      .addCase(uploadDriverJar.fulfilled, (state, action) => {
        state.loading = false;
        const driver = action.payload.data;
        state.driverJars = [...state.driverJars, driver];
        if (driver.active !== false) {
          state.activeDriverJars = [...state.activeDriverJars, driver];
        }
      })
      .addCase(uploadDriverJar.rejected, (state, action) => {
        state.loading = false;
        state.errorMessage = action.error.message;
      })
      .addCase(updateDriverJar.fulfilled, (state, action) => {
        state.loading = false;
        const driver = action.payload.data;
        const index = state.driverJars.findIndex(d => d.id === driver.id);
        if (index >= 0) {
          state.driverJars[index] = driver;
        }
        if (state.driverJar?.id === driver.id) {
          state.driverJar = driver;
        }
        if (driver.active !== false) {
          const activeIndex = state.activeDriverJars.findIndex(d => d.id === driver.id);
          if (activeIndex >= 0) {
            state.activeDriverJars[activeIndex] = driver;
          } else {
            state.activeDriverJars = [...state.activeDriverJars, driver];
          }
        } else {
          state.activeDriverJars = state.activeDriverJars.filter(d => d.id !== driver.id);
        }
      })
      .addCase(deleteDriverJar.fulfilled, (state, action) => {
        state.loading = false;
        const id = action.payload;
        state.driverJars = state.driverJars.filter(d => d.id !== id);
        state.activeDriverJars = state.activeDriverJars.filter(d => d.id !== id);
        if (state.driverJar?.id === id) {
          state.driverJar = null;
        }
      });
  },
});

export const { reset } = DriverJarManagementSlice.actions;

export default DriverJarManagementSlice.reducer;
